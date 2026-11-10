package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.MatchedPartDto;
import com.mottu.ops.application.dto.TriageResultPayloadDto;
import com.mottu.ops.application.port.in.AiTriageResultConsumerUseCase;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.domain.model.AiTriageDecision;
import com.mottu.ops.domain.model.DamageItem;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.domain.model.RepairBudget;
import com.mottu.ops.domain.service.AiConfidencePolicy;
import com.mottu.ops.domain.service.AiRecoveryPolicy;
import com.mottu.ops.domain.service.BudgetAndVerdictCalculator;
import com.mottu.ops.domain.service.PreExistingRuleCalculator;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AnalyticsOutboxJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionAuditLogJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionItemJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataAnalyticsOutboxRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionAuditLogRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionItemRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class AiTriageResultConsumerService implements AiTriageResultConsumerUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiTriageResultConsumerService.class);
    public static final String TRIAGE_RESULTS_QUEUE = "queue:ai_triage_results";

    private final SpringDataInspectionRepository inspectionRepository;
    private final SpringDataInspectionItemRepository inspectionItemRepository;
    private final SpringDataInspectionAuditLogRepository auditLogRepository;
    private final SpringDataAnalyticsOutboxRepository outboxRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public AiTriageResultConsumerService(
            SpringDataInspectionRepository inspectionRepository,
            SpringDataInspectionItemRepository inspectionItemRepository,
            SpringDataInspectionAuditLogRepository auditLogRepository,
            SpringDataAnalyticsOutboxRepository outboxRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            SseEmitterRegistry sseEmitterRegistry,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.outboxRepository = outboxRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void processAiResult(TriageResultPayloadDto payload) {
        if (payload == null || payload.inspectionId() == null) {
            return;
        }

        Optional<InspectionJpaEntity> opt = inspectionRepository.findById(payload.inspectionId());
        if (opt.isEmpty()) {
            log.warn("Received AI triage result for unknown inspection {}", payload.inspectionId());
            return;
        }

        InspectionJpaEntity entity = opt.get();
        if (entity.getStatus() != InspectionStatus.QUEUED_AI && entity.getStatus() != InspectionStatus.AWAITING_AI_RECOVERY) {
            log.info("Inspection {} is in state {}, ignoring AI result", entity.getId(), entity.getStatus());
            return;
        }

        String previousStatus = entity.getStatus().name();
        Instant now = Instant.now();

        // 1. Outcome == AI_UNAVAILABLE
        if ("AI_UNAVAILABLE".equalsIgnoreCase(payload.outcome())) {
            int nextRetry = entity.getRetryCount() + 1;
            Instant nextRetryAt = new AiRecoveryPolicy().calculateNextRetry(now, nextRetry);
            entity.setStatus(InspectionStatus.AWAITING_AI_RECOVERY);
            entity.setRetryCount(nextRetry);
            entity.setNextRetryAt(nextRetryAt);
            inspectionRepository.save(entity);

            saveAuditLog(entity.getId(), null, "SYSTEM_AI", previousStatus, "AWAITING_AI_RECOVERY", Map.of(
                    "action", "AI_UNAVAILABLE_FALLBACK",
                    "retryCount", nextRetry,
                    "nextRetryAt", nextRetryAt.toString()
            ));
            sseEmitterRegistry.emitStatusChange(entity.getId(), InspectionStatus.AWAITING_AI_RECOVERY);
            return;
        }

        // 2. Outcome == SUCCESS
        if ("SUCCESS".equalsIgnoreCase(payload.outcome())) {
            if (Boolean.FALSE.equals(payload.isValidMotorcyclePhoto())) {
                entity.setStatus(InspectionStatus.INVALID_IMAGE);
                entity.setAiConfidence(payload.confidence());
                entity.setAiTechnicalSummary(payload.technicalSummary());
                entity.setCustomerFeedbackText(payload.customerFriendlyMessage());
                entity.setResolvedAt(now);
                if (payload.committeeEvaluation() != null) {
                    try {
                        entity.setCommitteeEvaluationJson(objectMapper.writeValueAsString(payload.committeeEvaluation()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize committee evaluation JSON", e);
                    }
                }

                inspectionRepository.save(entity);

                saveAuditLog(entity.getId(), null, "SYSTEM_AI", previousStatus, "INVALID_IMAGE", Map.of(
                        "action", "AI_INVALID_IMAGE",
                        "technicalSummary", payload.technicalSummary() != null ? payload.technicalSummary() : ""
                ));
                sseEmitterRegistry.emitStatusChange(entity.getId(), InspectionStatus.INVALID_IMAGE);
                return;
            }

            // Valid motorcycle photo
            RentalContractJpaEntity contract = rentalContractRepository.findById(entity.getContractId()).orElse(null);
            double hours = 0.0;
            int km = 0;
            if (contract != null) {
                hours = Math.max(0.0, Duration.between(contract.getPickedUpAt(), now).toMinutes() / 60.0);
                km = Math.max(0, entity.getReportedOdometerKm() - contract.getInitialOdometerKm());
            }

            boolean isPreExistingEligible = (hours <= PreExistingRuleCalculator.MAX_HOURS || km <= PreExistingRuleCalculator.MAX_KM);

            AiConfidencePolicy confidencePolicy = new AiConfidencePolicy();
            BigDecimal confidence = payload.confidence() != null ? payload.confidence() : BigDecimal.ZERO;

            AiTriageDecision decision = confidencePolicy.evaluateTriage(
                    true,
                    confidence,
                    entity.getReportedContext(),
                    isPreExistingEligible,
                    entity.isFraudSuspected(),
                    entity.getSafetyRisk()
            );

            if (decision.targetStatus() == InspectionStatus.AUTO_RESOLVED) {
                BudgetAndVerdictCalculator budgetCalc = new BudgetAndVerdictCalculator();
                boolean allPartsNaturalWear = false;
                if (payload.matchedParts() != null && !payload.matchedParts().isEmpty()) {
                    allPartsNaturalWear = payload.matchedParts().stream()
                            .allMatch(p -> Boolean.TRUE.equals(p.isNaturalWear()));
                }

                DamageVerdict verdict = budgetCalc.determineVerdict(entity.getReportedContext(), isPreExistingEligible, allPartsNaturalWear);
                if (payload.suggestedVerdict() != null && entity.getReportedContext() != ReportedOriginContext.RECEIVED_LIKE_THIS) {
                    verdict = payload.suggestedVerdict();
                }

                List<DamageItem> damageItems = new ArrayList<>();
                if (payload.matchedParts() != null) {
                    for (MatchedPartDto p : payload.matchedParts()) {
                        damageItems.add(new DamageItem(
                                p.partName() != null ? p.partName() : "Peça",
                                Money.of(p.unitPriceBrl() != null ? p.unitPriceBrl() : BigDecimal.ZERO),
                                Boolean.TRUE.equals(p.isNaturalWear())
                        ));
                    }
                }

                RepairBudget budget = budgetCalc.calculateBudget(verdict, damageItems);

                // Save matched parts to inspection_items
                inspectionItemRepository.deleteByInspectionId(entity.getId());
                if (payload.matchedParts() != null) {
                    for (MatchedPartDto p : payload.matchedParts()) {
                        inspectionItemRepository.save(new InspectionItemJpaEntity(
                                UUID.randomUUID(),
                                entity.getId(),
                                p.catalogPartId(),
                                p.partName() != null ? p.partName() : "Peça",
                                p.unitPriceBrl() != null ? p.unitPriceBrl() : BigDecimal.ZERO,
                                "AI"
                        ));
                    }
                }

                entity.setStatus(InspectionStatus.AUTO_RESOLVED);
                entity.setAiVerdict(verdict);
                entity.setFinalVerdict(verdict);
                entity.setAiConfidence(confidence);
                entity.setLaborFeeBrl(budget.laborFee().getAmount());
                entity.setTotalEstimatedBrl(budget.totalEstimated().getAmount());
                entity.setAiTechnicalSummary(payload.technicalSummary());
                entity.setCustomerFeedbackText(payload.customerFriendlyMessage());
                entity.setResolvedAt(now);

                if (payload.committeeEvaluation() != null) {
                    try {
                        entity.setCommitteeEvaluationJson(objectMapper.writeValueAsString(payload.committeeEvaluation()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize committee evaluation JSON", e);
                    }
                }

                if (payload.committeeEvaluation() != null) {
                    try {
                        entity.setCommitteeEvaluationJson(objectMapper.writeValueAsString(payload.committeeEvaluation()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize committee evaluation JSON", e);
                    }
                }

                inspectionRepository.save(entity);

                saveAuditLog(entity.getId(), null, "SYSTEM_AI", previousStatus, "AUTO_RESOLVED", Map.of(
                        "action", "AI_AUTO_RESOLVED",
                        "verdict", verdict.name(),
                        "totalEstimatedBrl", budget.totalEstimated().getAmount()
                ));

                saveAnalyticsOutbox(entity, true, now);
                sseEmitterRegistry.emitStatusChange(entity.getId(), InspectionStatus.AUTO_RESOLVED);
            } else {
                // Human Triage
                entity.setStatus(InspectionStatus.HUMAN_TRIAGE);
                entity.setPriorityReason(decision.priorityReason());
                entity.setPriorityWeight(decision.priorityWeight());
                entity.setAiConfidence(confidence);
                entity.setAiVerdict(payload.suggestedVerdict());
                entity.setAiTechnicalSummary(payload.technicalSummary());

                // Save matched parts for mechanic review
                inspectionItemRepository.deleteByInspectionId(entity.getId());
                if (payload.matchedParts() != null) {
                    for (MatchedPartDto p : payload.matchedParts()) {
                        inspectionItemRepository.save(new InspectionItemJpaEntity(
                                UUID.randomUUID(),
                                entity.getId(),
                                p.catalogPartId(),
                                p.partName() != null ? p.partName() : "Peça",
                                p.unitPriceBrl() != null ? p.unitPriceBrl() : BigDecimal.ZERO,
                                "AI"
                        ));
                    }
                }

                if (payload.committeeEvaluation() != null) {
                    try {
                        entity.setCommitteeEvaluationJson(objectMapper.writeValueAsString(payload.committeeEvaluation()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize committee evaluation JSON", e);
                    }
                }

                inspectionRepository.save(entity);

                saveAuditLog(entity.getId(), null, "SYSTEM_AI", previousStatus, "HUMAN_TRIAGE", Map.of(
                        "action", "ROUTED_TO_HUMAN_TRIAGE",
                        "reason", decision.priorityReason().name(),
                        "weight", decision.priorityWeight()
                ));

                sseEmitterRegistry.emitStatusChange(entity.getId(), InspectionStatus.HUMAN_TRIAGE);
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

    @Scheduled(fixedDelay = 2000)
    public void pollResultsQueue() {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return;
            }
            String rawJson = redisTemplate.opsForList().rightPop(TRIAGE_RESULTS_QUEUE);
            if (rawJson != null && !rawJson.isBlank()) {
                TriageResultPayloadDto payload = objectMapper.readValue(rawJson, TriageResultPayloadDto.class);
                processAiResult(payload);
            }
        } catch (Exception e) {
            log.debug("Error or timeout polling Redis triage results: {}", e.getMessage());
        }
    }
}
