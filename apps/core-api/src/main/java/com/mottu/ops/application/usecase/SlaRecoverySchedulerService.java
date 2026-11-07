package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.TriageJobPayloadDto;
import com.mottu.ops.application.port.in.SlaRecoverySchedulerUseCase;
import com.mottu.ops.application.port.out.AiTriageQueuePort;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.model.AiTriageDecision;
import com.mottu.ops.domain.service.AiRecoveryPolicy;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionAuditLogJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionAuditLogRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SlaRecoverySchedulerService implements SlaRecoverySchedulerUseCase {

    private static final Logger log = LoggerFactory.getLogger(SlaRecoverySchedulerService.class);

    private final SpringDataInspectionRepository inspectionRepository;
    private final SpringDataInspectionAuditLogRepository auditLogRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final AiTriageQueuePort aiTriageQueuePort;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    public SlaRecoverySchedulerService(
            SpringDataInspectionRepository inspectionRepository,
            SpringDataInspectionAuditLogRepository auditLogRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            AiTriageQueuePort aiTriageQueuePort,
            SseEmitterRegistry sseEmitterRegistry,
            ObjectMapper objectMapper
    ) {
        this.inspectionRepository = inspectionRepository;
        this.auditLogRepository = auditLogRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.aiTriageQueuePort = aiTriageQueuePort;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 15000)
    public void evaluateRecoverySla() {
        List<InspectionJpaEntity> recovering = inspectionRepository.findByStatus(InspectionStatus.AWAITING_AI_RECOVERY);
        if (recovering.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        AiRecoveryPolicy recoveryPolicy = new AiRecoveryPolicy();

        for (InspectionJpaEntity entity : recovering) {
            AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                    entity.getSafetyRisk(),
                    entity.getCreatedAt(),
                    now,
                    entity.getRetryCount()
            );

            if (decision.targetStatus() == InspectionStatus.HUMAN_TRIAGE) {
                String previousStatus = entity.getStatus().name();
                entity.setStatus(InspectionStatus.HUMAN_TRIAGE);
                entity.setPriorityReason(decision.priorityReason());
                entity.setPriorityWeight(decision.priorityWeight());
                inspectionRepository.save(entity);

                saveAuditLog(entity.getId(), null, "SYSTEM_AI", previousStatus, "HUMAN_TRIAGE", Map.of(
                        "action", "SLA_RECOVERY_ESCALATION",
                        "reason", decision.priorityReason().name(),
                        "weight", decision.priorityWeight()
                ));
                sseEmitterRegistry.emitStatusChange(entity.getId(), InspectionStatus.HUMAN_TRIAGE);
            } else {
                // If retry eligible
                if (entity.getRetryCount() < AiRecoveryPolicy.MAX_AI_RETRIES && entity.getNextRetryAt() != null && !entity.getNextRetryAt().isAfter(now)) {
                    RentalContractJpaEntity contract = rentalContractRepository.findById(entity.getContractId()).orElse(null);
                    MotorcycleJpaEntity moto = (contract != null) ? motorcycleRepository.findById(contract.getMotorcycleId()).orElse(null) : null;
                    MotorcycleModel model = moto != null ? moto.getModel() : MotorcycleModel.MOTTU_SPORT_110I;
                    double hoursSincePickup = contract != null ? Math.max(0.0, Duration.between(contract.getPickedUpAt(), now).toMinutes() / 60.0) : 0.0;
                    int kmDriven = contract != null ? Math.max(0, entity.getReportedOdometerKm() - contract.getInitialOdometerKm()) : 0;

                    TriageJobPayloadDto job = new TriageJobPayloadDto(
                            entity.getId(),
                            UUID.randomUUID(),
                            entity.getPhotoObjectKey(),
                            model,
                            entity.getReportedContext(),
                            hoursSincePickup,
                            kmDriven,
                            entity.getRiderNotes()
                    );
                    aiTriageQueuePort.publishJob(job);

                    int nextRetry = entity.getRetryCount() + 1;
                    Instant nextRetryAt = recoveryPolicy.calculateNextRetry(now, nextRetry);
                    entity.setRetryCount(nextRetry);
                    entity.setNextRetryAt(nextRetryAt);
                    inspectionRepository.save(entity);
                }
            }
        }
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
