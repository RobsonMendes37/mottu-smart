package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.MechanicWorkbenchUseCase;
import com.mottu.ops.application.port.out.FeedbackPolisherPort;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import com.mottu.ops.infrastructure.adapter.out.storage.S3StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class MechanicWorkbenchService implements MechanicWorkbenchUseCase {

    private static final Logger log = LoggerFactory.getLogger(MechanicWorkbenchService.class);
    private static final BigDecimal STANDARD_LABOR_FEE = BigDecimal.valueOf(35.00);

    private final SpringDataInspectionRepository inspectionRepository;
    private final SpringDataInspectionItemRepository inspectionItemRepository;
    private final SpringDataInspectionAuditLogRepository auditLogRepository;
    private final SpringDataAnalyticsOutboxRepository outboxRepository;
    private final SpringDataCatalogPartRepository catalogPartRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final FeedbackPolisherPort feedbackPolisherPort;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final S3StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    public MechanicWorkbenchService(
            SpringDataInspectionRepository inspectionRepository,
            SpringDataInspectionItemRepository inspectionItemRepository,
            SpringDataInspectionAuditLogRepository auditLogRepository,
            SpringDataAnalyticsOutboxRepository outboxRepository,
            SpringDataCatalogPartRepository catalogPartRepository,
            SpringDataUserRepository userRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            FeedbackPolisherPort feedbackPolisherPort,
            SseEmitterRegistry sseEmitterRegistry,
            S3StorageProperties storageProperties,
            ObjectMapper objectMapper
    ) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.outboxRepository = outboxRepository;
        this.catalogPartRepository = catalogPartRepository;
        this.userRepository = userRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.feedbackPolisherPort = feedbackPolisherPort;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MechanicQueueItemDto> getQueue(QueuePriorityReason reason, String plate, Pageable pageable) {
        Page<InspectionJpaEntity> page = inspectionRepository.findMechanicQueue(InspectionStatus.HUMAN_TRIAGE, reason, pageable);

        List<MechanicQueueItemDto> items = page.getContent().stream()
                .map(inspection -> {
                    MotorcycleJpaEntity moto = motorcycleRepository.findById(inspection.getMotorcycleId()).orElse(null);
                    UserJpaEntity rider = userRepository.findById(inspection.getRiderId()).orElse(null);

                    String motoPlate = (moto != null) ? moto.getPlate() : "PLACA-N/A";
                    MotorcycleModel model = (moto != null) ? moto.getModel() : MotorcycleModel.MOTTU_SPORT_110I;
                    String riderName = (rider != null) ? rider.getFullName() : "Entregador";

                    return new MechanicQueueItemDto(
                            inspection.getId(),
                            motoPlate,
                            model,
                            riderName,
                            inspection.getPriorityWeight(),
                            inspection.getPriorityReason(),
                            inspection.getSafetyRisk(),
                            inspection.isFraudSuspected(),
                            inspection.getAiConfidence(),
                            inspection.getAiVerdict(),
                            inspection.getCreatedAt()
                    );
                })
                .filter(item -> plate == null || plate.isBlank() || item.plate().toUpperCase().contains(plate.trim().toUpperCase()))
                .toList();

        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public MechanicInspectionDetailsResponse getDetails(UUID inspectionId) {
        InspectionJpaEntity entity = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId));

        MotorcycleJpaEntity moto = motorcycleRepository.findById(entity.getMotorcycleId()).orElse(null);
        UserJpaEntity rider = userRepository.findById(entity.getRiderId()).orElse(null);
        RentalContractJpaEntity contract = rentalContractRepository.findById(entity.getContractId()).orElse(null);

        String plate = moto != null ? moto.getPlate() : "PLACA-N/A";
        MotorcycleModel model = moto != null ? moto.getModel() : MotorcycleModel.MOTTU_SPORT_110I;
        String riderName = rider != null ? rider.getFullName() : "Entregador";
        String riderPhone = rider != null ? rider.getPhone() : "";

        double hoursSincePickup = 0.0;
        int kmDriven = 0;
        if (contract != null) {
            hoursSincePickup = Math.max(0.0, Duration.between(contract.getPickedUpAt(), entity.getCreatedAt()).toMinutes() / 60.0);
            kmDriven = Math.max(0, entity.getReportedOdometerKm() - contract.getInitialOdometerKm());
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

        return new MechanicInspectionDetailsResponse(
                entity.getId(),
                entity.getStatus(),
                plate,
                model,
                riderName,
                riderPhone,
                hoursSincePickup,
                kmDriven,
                photoDownloadUrl,
                entity.isFraudSuspected(),
                entity.getFraudReason(),
                entity.getReportedContext(),
                entity.getSafetyRisk(),
                entity.getReportedOdometerKm(),
                entity.getRiderNotes(),
                entity.getAiVerdict(),
                entity.getAiConfidence(),
                entity.getAiTechnicalSummary(),
                entity.isContestedOnce(),
                entity.getContestationReason(),
                entity.getFinalVerdict(),
                entity.getLaborFeeBrl(),
                entity.getTotalEstimatedBrl(),
                entity.getCustomerFeedbackText(),
                items,
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }

    @Override
    @Transactional
    public ResolveInspectionResponse resolve(UUID mechanicId, UUID inspectionId, ResolveInspectionRequest request) {
        InspectionJpaEntity entity = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vistoria não encontrada: " + inspectionId));

        if (entity.getStatus() != InspectionStatus.HUMAN_TRIAGE) {
            throw new BusinessRuleViolationException("Apenas chamados em HUMAN_TRIAGE podem ser finalizados pelo mecânico");
        }

        List<CatalogPartJpaEntity> selectedParts = Collections.emptyList();
        if (request.catalogPartIds() != null && !request.catalogPartIds().isEmpty()) {
            selectedParts = catalogPartRepository.findAllById(request.catalogPartIds());
        }

        BigDecimal laborFee = BigDecimal.ZERO;
        BigDecimal totalEstimated = BigDecimal.ZERO;

        if (request.finalVerdict() == DamageVerdict.MAU_USO) {
            BigDecimal partsSum = selectedParts.stream()
                    .map(CatalogPartJpaEntity::getUnitPriceBrl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            laborFee = STANDARD_LABOR_FEE;
            totalEstimated = partsSum.add(laborFee);
        }

        List<String> partNames = selectedParts.stream().map(CatalogPartJpaEntity::getPartName).toList();
        String customerFeedback = feedbackPolisherPort.polishFeedback(
                request.finalVerdict(),
                partNames,
                totalEstimated,
                request.mechanicNotes()
        );

        // Replace inspection items with mechanic selected parts
        inspectionItemRepository.deleteByInspectionId(inspectionId);
        for (CatalogPartJpaEntity part : selectedParts) {
            inspectionItemRepository.save(new InspectionItemJpaEntity(
                    UUID.randomUUID(),
                    inspectionId,
                    part.getId(),
                    part.getPartName(),
                    part.getUnitPriceBrl(),
                    "MECHANIC"
            ));
        }

        String previousStatus = entity.getStatus().name();
        Instant now = Instant.now();

        entity.setStatus(InspectionStatus.HUMAN_RESOLVED);
        entity.setFinalVerdict(request.finalVerdict());
        entity.setLaborFeeBrl(laborFee);
        entity.setTotalEstimatedBrl(totalEstimated);
        entity.setReviewedByMechanicId(mechanicId);
        entity.setMechanicNotes(request.mechanicNotes());
        entity.setCustomerFeedbackText(customerFeedback);
        entity.setResolvedAt(now);

        inspectionRepository.save(entity);

        // Record Audit Log
        saveAuditLog(inspectionId, mechanicId, "MECHANIC", previousStatus, "HUMAN_RESOLVED", Map.of(
                "action", "MECHANIC_RESOLVED",
                "finalVerdict", request.finalVerdict().name(),
                "totalEstimatedBrl", totalEstimated,
                "mechanicNotes", request.mechanicNotes() != null ? request.mechanicNotes() : ""
        ));

        // Record Analytics Outbox
        boolean aiMatchedMechanic = (entity.getAiVerdict() != null && entity.getAiVerdict() == request.finalVerdict());
        saveAnalyticsOutbox(entity, aiMatchedMechanic, now);

        // Emit SSE
        sseEmitterRegistry.emitStatusChange(inspectionId, InspectionStatus.HUMAN_RESOLVED);

        return new ResolveInspectionResponse(
                inspectionId,
                InspectionStatus.HUMAN_RESOLVED,
                request.finalVerdict(),
                totalEstimated,
                customerFeedback
        );
    }

    private String buildPhotoDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || "null".equalsIgnoreCase(objectKey.trim())) {
            return null;
        }
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

    private void saveAnalyticsOutbox(InspectionJpaEntity entity, boolean aiMatchedMechanic, Instant resolvedAt) {
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("inspectionId", entity.getId().toString());
            payload.put("riderId", entity.getRiderId().toString());
            payload.put("motorcycleId", entity.getMotorcycleId().toString());
            payload.put("aiVerdict", entity.getAiVerdict() != null ? entity.getAiVerdict().name() : "NONE");
            payload.put("finalVerdict", entity.getFinalVerdict() != null ? entity.getFinalVerdict().name() : "NONE");
            payload.put("aiMatchedMechanic", aiMatchedMechanic);
            payload.put("totalEstimatedBrl", entity.getTotalEstimatedBrl());
            payload.put("laborFeeBrl", entity.getLaborFeeBrl());
            payload.put("resolvedAt", resolvedAt.toString());

            String payloadJson = objectMapper.writeValueAsString(payload);
            outboxRepository.save(new AnalyticsOutboxJpaEntity(
                    UUID.randomUUID(),
                    entity.getId(),
                    payloadJson,
                    false,
                    resolvedAt,
                    null
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to record analytics outbox for inspection {}", entity.getId(), e);
        }
    }
}
