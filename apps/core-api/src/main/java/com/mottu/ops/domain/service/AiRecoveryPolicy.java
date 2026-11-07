package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.model.AiTriageDecision;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class AiRecoveryPolicy {

    public static final int MAX_AI_RETRIES = 3;
    public static final Duration CRITICAL_SAFETY_SLA = Duration.ofMinutes(5);

    /**
     * Computes the next retry timestamp following exponential backoff:
     * - retry 0 -> +30s
     * - retry 1 -> +60s
     * - retry 2 -> +300s (5m)
     */
    public Instant calculateNextRetry(Instant fromTime, int currentRetryCount) {
        Objects.requireNonNull(fromTime, "fromTime cannot be null");
        return switch (currentRetryCount) {
            case 0 -> fromTime.plusSeconds(30);
            case 1 -> fromTime.plusSeconds(60);
            default -> fromTime.plusSeconds(300);
        };
    }

    /**
     * Evaluates SLA timeout for tickets waiting in AWAITING_AI_RECOVERY:
     * - If CRITICAL_STOPPED and elapsed time >= 5 minutes -> escalate to HUMAN_TRIAGE (CRITICAL_SAFETY, weight 1).
     * - If retries exhausted (>= MAX_AI_RETRIES) -> escalate to HUMAN_TRIAGE (LOW_AI_CONFIDENCE or CRITICAL_SAFETY).
     * - Otherwise -> remain in AWAITING_AI_RECOVERY.
     */
    public AiTriageDecision evaluateRecoverySla(
            SafetyRiskLevel safetyRisk,
            Instant createdAt,
            Instant currentTime,
            int retryCount
    ) {
        Objects.requireNonNull(safetyRisk, "SafetyRiskLevel cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        Objects.requireNonNull(currentTime, "currentTime cannot be null");

        Duration elapsed = Duration.between(createdAt, currentTime);

        if (safetyRisk == SafetyRiskLevel.CRITICAL_STOPPED && elapsed.compareTo(CRITICAL_SAFETY_SLA) >= 0) {
            return AiTriageDecision.humanTriage(QueuePriorityReason.CRITICAL_SAFETY);
        }

        if (retryCount >= MAX_AI_RETRIES) {
            QueuePriorityReason reason = (safetyRisk == SafetyRiskLevel.CRITICAL_STOPPED)
                    ? QueuePriorityReason.CRITICAL_SAFETY
                    : QueuePriorityReason.LOW_AI_CONFIDENCE;
            return AiTriageDecision.humanTriage(reason);
        }

        return AiTriageDecision.awaitingRecovery();
    }
}
