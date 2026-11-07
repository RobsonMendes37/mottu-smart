package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.AiTriageDecision;

import java.math.BigDecimal;
import java.util.Objects;

public class AiConfidencePolicy {

    public static final BigDecimal MINIMUM_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.80);

    public AiTriageDecision evaluateTriage(
            boolean isValidMotorcyclePhoto,
            BigDecimal confidenceScore,
            ReportedOriginContext context,
            boolean isPreExistingEligible,
            boolean fraudSuspected,
            SafetyRiskLevel safetyRisk
    ) {
        if (!isValidMotorcyclePhoto) {
            return AiTriageDecision.invalidImage();
        }

        Objects.requireNonNull(confidenceScore, "confidenceScore cannot be null for valid motorcycle photo");
        if (confidenceScore.compareTo(BigDecimal.ZERO) < 0 || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new DomainValidationException("Confidence score must be between 0.0 and 1.0: " + confidenceScore);
        }

        if (fraudSuspected) {
            return AiTriageDecision.humanTriage(QueuePriorityReason.FRAUD_SUSPICION);
        }

        if (context == ReportedOriginContext.RECEIVED_LIKE_THIS && !isPreExistingEligible) {
            QueuePriorityReason reason = (safetyRisk == SafetyRiskLevel.CRITICAL_STOPPED)
                    ? QueuePriorityReason.CRITICAL_SAFETY
                    : QueuePriorityReason.LOW_AI_CONFIDENCE;
            return AiTriageDecision.humanTriage(reason);
        }

        if (confidenceScore.compareTo(MINIMUM_CONFIDENCE_THRESHOLD) < 0) {
            QueuePriorityReason reason = (safetyRisk == SafetyRiskLevel.CRITICAL_STOPPED)
                    ? QueuePriorityReason.CRITICAL_SAFETY
                    : QueuePriorityReason.LOW_AI_CONFIDENCE;
            return AiTriageDecision.humanTriage(reason);
        }

        return AiTriageDecision.autoResolved();
    }
}
