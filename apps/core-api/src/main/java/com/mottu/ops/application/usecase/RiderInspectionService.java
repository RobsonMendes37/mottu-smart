package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.RiderInspectionUseCase;
import com.mottu.ops.application.port.out.AiTriageQueuePort;
import com.mottu.ops.application.port.out.StoragePort;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.PhotoHash;
import com.mottu.ops.domain.service.PhotoFraudDetector;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionAuditLogJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.domain.model.YardAppointment;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardAppointmentJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionAuditLogRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionItemRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataYardAppointmentRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataYardRepository;
import com.mottu.ops.infrastructure.adapter.out.storage.PerceptualHashService;
import com.mottu.ops.infrastructure.adapter.out.storage.S3StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class RiderInspectionService implements RiderInspectionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RiderInspectionService.class);
    private static final Set<InspectionStatus> OPEN_STATUSES = Set.of(
            InspectionStatus.QUEUED_AI,
            InspectionStatus.AWAITING_AI_RECOVERY,
            InspectionStatus.HUMAN_TRIAGE
    );

    private final SpringDataInspectionRepository inspectionRepository;
    private final SpringDataInspectionItemRepository inspectionItemRepository;
    private final SpringDataInspectionAuditLogRepository auditLogRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataYardAppointmentRepository appointmentRepository;
    private final SpringDataYardRepository yardRepository;
    private final StoragePort storagePort;
    private final S3StorageProperties storageProperties;
    private final PerceptualHashService perceptualHashService;
    private final AiTriageQueuePort aiTriageQueuePort;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    public RiderInspectionService(
            SpringDataInspectionRepository inspectionRepository,
            SpringDataInspectionItemRepository inspectionItemRepository,
            SpringDataInspectionAuditLogRepository auditLogRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataYardAppointmentRepository appointmentRepository,
            SpringDataYardRepository yardRepository,
            StoragePort storagePort,
            S3StorageProperties storageProperties,
            PerceptualHashService perceptualHashService,
            AiTriageQueuePort aiTriageQueuePort,
            SseEmitterRegistry sseEmitterRegistry,
            ObjectMapper objectMapper
    ) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.appointmentRepository = appointmentRepository;
        this.yardRepository = yardRepository;
        this.storagePort = storagePort;
        this.storageProperties = storageProperties;
        this.perceptualHashService = perceptualHashService;
        this.aiTriageQueuePort = aiTriageQueuePort;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreateInspectionResponse createInspection(UUID riderId, String idempotencyKey, CreateInspectionRequest request) {
        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<InspectionJpaEntity> existing = inspectionRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                InspectionJpaEntity e = existing.get();
                return new CreateInspectionResponse(e.getId(), e.getStatus());
            }
        }

        // 2. Active Rental Contract
        RentalContractJpaEntity contract = rentalContractRepository.findByRiderIdAndActiveTrue(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum contrato ativo encontrado para o entregador"));

        // 3. Quota: Max 3 open inspections per contract
        int openCount = inspectionRepository.countByContractIdAndStatusIn(contract.getId(), OPEN_STATUSES);
        if (openCount >= 3) {
            throw new BusinessRuleViolationException("Limite de 3 chamados em aberto atingido");
        }

        // 4. Photo Hashing & Anti-Fraud
        byte[] photoBytes = storagePort.getObjectBytes(request.photoObjectKey());
        String sha256 = PhotoFraudDetector.computeSha256(photoBytes);
        String perceptualHash = perceptualHashService.computeDHash(photoBytes, sha256);

        boolean sha256Collision = inspectionRepository.existsByPhotoSha256(sha256);
        boolean dHashCollision = false;
        if (!sha256Collision && perceptualHash != null) {
            List<String> existingHashes = inspectionRepository.findAllPhotoPerceptualHashes();
            for (String pastHash : existingHashes) {
                if (pastHash != null && pastHash.length() == perceptualHash.length()) {
                    try {
                        int dist = PhotoHash.calculateHammingDistance(perceptualHash, pastHash);
                        if (dist <= PhotoFraudDetector.NEAR_DUPLICATE_THRESHOLD) {
                            dHashCollision = true;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        boolean fraudSuspected = sha256Collision || dHashCollision;
        String fraudReason = fraudSuspected
                ? (sha256Collision ? "PHOTO_FRAUD_DETECTED: SHA-256 match" : "PHOTO_FRAUD_DETECTED: visual dHash match")
                : null;

        InspectionStatus status = fraudSuspected ? InspectionStatus.HUMAN_TRIAGE : InspectionStatus.QUEUED_AI;
        QueuePriorityReason priorityReason = fraudSuspected ? QueuePriorityReason.FRAUD_SUSPICION : null;
        Integer priorityWeight = fraudSuspected ? QueuePriorityReason.FRAUD_SUSPICION.getWeight() : null;

        UUID inspectionId = UUID.randomUUID();
        Instant now = Instant.now();

        InspectionJpaEntity entity = new InspectionJpaEntity();
        entity.setId(inspectionId);
        entity.setIdempotencyKey((idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : "auto-" + inspectionId);
        entity.setContractId(contract.getId());
        entity.setRiderId(riderId);
        entity.setMotorcycleId(contract.getMotorcycleId());
        entity.setPhotoObjectKey(request.photoObjectKey());
        entity.setPhotoSha256(sha256);
        entity.setPhotoPerceptualHash(perceptualHash);
        entity.setFraudSuspected(fraudSuspected);
        entity.setFraudReason(fraudReason);
        entity.setReportedContext(request.reportedContext());
        entity.setSafetyRisk(request.safetyRisk());
        entity.setReportedOdometerKm(request.reportedOdometerKm());
        entity.setRiderNotes(request.riderNotes());
        entity.setStatus(status);
        entity.setPriorityReason(priorityReason);
        entity.setPriorityWeight(priorityWeight);
        entity.setLaborFeeBrl(BigDecimal.ZERO);
        entity.setTotalEstimatedBrl(BigDecimal.ZERO);
        entity.setContestedOnce(false);
        entity.setRetryCount(0);
        entity.setCreatedAt(now);

        inspectionRepository.save(entity);

        // Record Initial Audit Log
        saveAuditLog(inspectionId, riderId, "RIDER", null, status.name(), Map.of(
                "action", "INSPECTION_CREATED",
                "fraudSuspected", fraudSuspected,
                "reportedContext", request.reportedContext().name(),
                "safetyRisk", request.safetyRisk().name()
        ));

        // Dispatch AI Triage Job if Clean
        if (!fraudSuspected) {
            MotorcycleJpaEntity moto = motorcycleRepository.findById(contract.getMotorcycleId()).orElse(null);
            MotorcycleModel model = moto != null ? moto.getModel() : MotorcycleModel.MOTTU_SPORT_110I;
            double hoursSincePickup = Math.max(0.0, Duration.between(contract.getPickedUpAt(), now).toMinutes() / 60.0);
            int kmDriven = Math.max(0, request.reportedOdometerKm() - contract.getInitialOdometerKm());

            TriageJobPayloadDto job = new TriageJobPayloadDto(
                    inspectionId,
                    UUID.randomUUID(),
                    request.photoObjectKey(),
                    model,
                    request.reportedContext(),
                    hoursSincePickup,
                    kmDriven,
                    request.riderNotes()
            );
            aiTriageQueuePort.publishJob(job);
        }

        // Emit SSE Status
        sseEmitterRegistry.emitStatusChange(inspectionId, status);

        return new CreateInspectionResponse(inspectionId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public RiderInspectionDetailsResponse getInspectionDetails(UUID riderId, UUID inspectionId) {
        InspectionJpaEntity entity = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId));

        // IDOR Protection: Rider cannot access other riders' inspections
        if (!entity.getRiderId().equals(riderId)) {
            throw new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId);
        }

        String photoDownloadUrl = buildPhotoDownloadUrl(entity.getPhotoObjectKey());

        List<InspectionItemDetailDto> items = inspectionItemRepository.findByInspectionId(inspectionId).stream()
                .map(item -> new InspectionItemDetailDto(
                        item.getCatalogPartId(),
                        item.getPartNameSnapshot(),
                        item.getUnitPriceSnapshotBrl(),
                        item.getAddedBy()
                ))
                .toList();

        boolean canContest = (entity.getStatus() == InspectionStatus.AUTO_RESOLVED && !entity.isContestedOnce());

        AppointmentDetailsDto appointmentDto = appointmentRepository.findByInspectionId(entity.getId())
                .map(apt -> {
                    String yardName = yardRepository.findById(apt.getYardId())
                            .map(YardJpaEntity::getName)
                            .orElse("Galpão Mottu");
                    return new AppointmentDetailsDto(
                            apt.getId(),
                            apt.getYardId(),
                            yardName,
                            apt.getScheduledDate(),
                            apt.getShift(),
                            apt.getCreatedAt()
                    );
                })
                .orElse(null);

        return new RiderInspectionDetailsResponse(
                entity.getId(),
                entity.getStatus(),
                photoDownloadUrl,
                entity.getReportedContext(),
                entity.getSafetyRisk(),
                entity.getReportedOdometerKm(),
                entity.getRiderNotes(),
                entity.getFinalVerdict(),
                entity.getAiConfidence(),
                entity.getLaborFeeBrl(),
                entity.getTotalEstimatedBrl(),
                entity.getCustomerFeedbackText(),
                canContest,
                entity.isContestedOnce(),
                items,
                appointmentDto,
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }

    @Override
    @Transactional
    public ContestInspectionResponse contestInspection(UUID riderId, UUID inspectionId, ContestInspectionRequest request) {
        InspectionJpaEntity entity = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId));

        if (!entity.getRiderId().equals(riderId)) {
            throw new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId);
        }

        if (entity.isContestedOnce()) {
            throw new BusinessRuleViolationException("Inspection was already contested once");
        }

        if (entity.getStatus() != InspectionStatus.AUTO_RESOLVED) {
            throw new BusinessRuleViolationException("Only AUTO_RESOLVED inspections can be contested");
        }

        if (request.reason() == null || request.reason().trim().length() < 10) {
            throw new DomainValidationException("Contestation reason must contain at least 10 non-whitespace characters");
        }

        String previousStatus = entity.getStatus().name();
        entity.setContestedOnce(true);
        entity.setContestationReason(request.reason().trim());
        entity.setStatus(InspectionStatus.HUMAN_TRIAGE);
        entity.setPriorityReason(QueuePriorityReason.RIDER_CONTESTATION);
        entity.setPriorityWeight(entity.getSafetyRisk() == SafetyRiskLevel.CRITICAL_STOPPED ? 1 : 2);

        inspectionRepository.save(entity);

        saveAuditLog(inspectionId, riderId, "RIDER", previousStatus, "HUMAN_TRIAGE", Map.of(
                "action", "RIDER_CONTESTED",
                "reason", request.reason().trim()
        ));

        sseEmitterRegistry.emitStatusChange(inspectionId, InspectionStatus.HUMAN_TRIAGE);

        return new ContestInspectionResponse(inspectionId, InspectionStatus.HUMAN_TRIAGE, QueuePriorityReason.RIDER_CONTESTATION);
    }

    @Override
    @Transactional
    public CreateAppointmentResponse createAppointment(UUID riderId, UUID inspectionId, CreateAppointmentRequest request) {
        if (request == null) {
            throw new DomainValidationException("Appointment request cannot be null");
        }
        if (request.yardId() == null) {
            throw new DomainValidationException("Yard ID cannot be null");
        }
        if (request.scheduledDate() == null) {
            throw new DomainValidationException("Scheduled date cannot be null");
        }
        if (request.scheduledDate().isBefore(LocalDate.now())) {
            throw new DomainValidationException("A data do agendamento não pode ser no passado");
        }
        if (request.shift() == null) {
            throw new DomainValidationException("Appointment shift cannot be null");
        }

        InspectionJpaEntity entity = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId));

        if (!entity.getRiderId().equals(riderId)) {
            throw new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId);
        }

        if (entity.getStatus() != InspectionStatus.AUTO_RESOLVED && entity.getStatus() != InspectionStatus.HUMAN_RESOLVED) {
            throw new BusinessRuleViolationException("Apenas vistorias resolvidas podem ter agendamento de galpão");
        }

        YardJpaEntity yard = yardRepository.findById(request.yardId())
                .filter(YardJpaEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Galpão não encontrado ou inativo"));

        YardAppointment domainAppointment = new YardAppointment(
                UUID.randomUUID(),
                inspectionId,
                yard.getId(),
                request.scheduledDate(),
                request.shift(),
                Instant.now()
        );

        YardAppointmentJpaEntity appointmentEntity = appointmentRepository.findByInspectionId(inspectionId)
                .orElseGet(YardAppointmentJpaEntity::new);

        appointmentEntity.setId(domainAppointment.getId());
        appointmentEntity.setInspectionId(inspectionId);
        appointmentEntity.setYardId(yard.getId());
        appointmentEntity.setScheduledDate(request.scheduledDate());
        appointmentEntity.setShift(request.shift());
        appointmentEntity.setCreatedAt(domainAppointment.getCreatedAt());

        appointmentRepository.save(appointmentEntity);

        String previousStatus = entity.getStatus().name();
        entity.setStatus(InspectionStatus.SCHEDULED);
        inspectionRepository.save(entity);

        saveAuditLog(inspectionId, riderId, "RIDER", previousStatus, "SCHEDULED", Map.of(
                "action", "SCHEDULED_APPOINTMENT",
                "appointmentId", domainAppointment.getId().toString(),
                "yardId", yard.getId().toString(),
                "yardName", yard.getName(),
                "scheduledDate", request.scheduledDate().toString(),
                "shift", request.shift().name()
        ));

        sseEmitterRegistry.emitStatusChange(inspectionId, InspectionStatus.SCHEDULED);

        return new CreateAppointmentResponse(
                domainAppointment.getId(),
                InspectionStatus.SCHEDULED,
                yard.getName(),
                request.scheduledDate(),
                request.shift()
        );
    }

    @Override
    public SseEmitter subscribeLiveStatus(UUID inspectionId) {
        InspectionJpaEntity entity = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId));

        return sseEmitterRegistry.registerEmitter(inspectionId, entity.getStatus());
    }

    private String buildPhotoDownloadUrl(String objectKey) {
        String base = storageProperties.getPublicEndpoint();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + storageProperties.getBucketName() + "/" + objectKey;
    }

    private void saveAuditLog(UUID inspectionId, UUID actorId, String actorRole, String prev, String next, Object details) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            auditLogRepository.save(new InspectionAuditLogJpaEntity(
                    UUID.randomUUID(),
                    inspectionId,
                    actorId,
                    actorRole,
                    prev,
                    next,
                    detailsJson,
                    Instant.now()
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log details for inspection {}", inspectionId, e);
        }
    }
}
