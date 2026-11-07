package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.AiTriageDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfidencePolicyTest {

    private AiConfidencePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AiConfidencePolicy();
    }

    @Test
    @DisplayName("TC-AIR-01 (UC-07): Invalid motorcycle photo routes to INVALID_IMAGE")
    void shouldRouteToInvalidImageWhenNotMotorcycle() {
        AiTriageDecision decision = policy.evaluateTriage(
                false, // not a valid motorcycle photo
                null,
                ReportedOriginContext.ACCIDENT_OR_DROP,
                false,
                false,
                SafetyRiskLevel.SAFE_TO_RIDE
        );

        assertEquals(InspectionStatus.INVALID_IMAGE, decision.targetStatus());
        assertFalse(decision.requiresHumanTriage());
        assertNull(decision.priorityReason());
        assertNull(decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-02 (UC-07): Confidence exactly 0.800 routes to AUTO_RESOLVED")
    void shouldAutoResolveWhenConfidenceReachesThreshold() {
        AiTriageDecision decision = policy.evaluateTriage(
                true,
                BigDecimal.valueOf(0.800),
                ReportedOriginContext.NORMAL_USE_WEAR,
                false,
                false,
                SafetyRiskLevel.SAFE_TO_RIDE
        );

        assertEquals(InspectionStatus.AUTO_RESOLVED, decision.targetStatus());
        assertFalse(decision.requiresHumanTriage());
    }

    @Test
    @DisplayName("TC-AIR-03 (UC-07): Confidence 0.799 on SAFE_TO_RIDE routes to HUMAN_TRIAGE with LOW_AI_CONFIDENCE (weight 3)")
    void shouldRouteToHumanTriageOnLowConfidenceSafeToRide() {
        AiTriageDecision decision = policy.evaluateTriage(
                true,
                BigDecimal.valueOf(0.799),
                ReportedOriginContext.NORMAL_USE_WEAR,
                false,
                false,
                SafetyRiskLevel.SAFE_TO_RIDE
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertTrue(decision.requiresHumanTriage());
        assertEquals(QueuePriorityReason.LOW_AI_CONFIDENCE, decision.priorityReason());
        assertEquals(3, decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-04 (UC-07): Confidence 0.799 on CRITICAL_STOPPED routes to HUMAN_TRIAGE with CRITICAL_SAFETY (weight 1)")
    void shouldRouteToHumanTriageOnLowConfidenceCriticalStopped() {
        AiTriageDecision decision = policy.evaluateTriage(
                true,
                BigDecimal.valueOf(0.799),
                ReportedOriginContext.NORMAL_USE_WEAR,
                false,
                false,
                SafetyRiskLevel.CRITICAL_STOPPED
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertTrue(decision.requiresHumanTriage());
        assertEquals(QueuePriorityReason.CRITICAL_SAFETY, decision.priorityReason());
        assertEquals(1, decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-05 (UC-04): Fraud suspected routes to HUMAN_TRIAGE with FRAUD_SUSPICION (weight 4) even if high confidence")
    void shouldRouteToHumanTriageOnFraudEvenWithHighConfidence() {
        AiTriageDecision decision = policy.evaluateTriage(
                true,
                BigDecimal.valueOf(0.99),
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                true,
                true, // fraud!
                SafetyRiskLevel.SAFE_TO_RIDE
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertTrue(decision.requiresHumanTriage());
        assertEquals(QueuePriorityReason.FRAUD_SUSPICION, decision.priorityReason());
        assertEquals(4, decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-06: Out of bounds confidence scores throw DomainValidationException")
    void shouldThrowOnOutOfBoundsConfidence() {
        assertThrows(DomainValidationException.class, () ->
                policy.evaluateTriage(true, BigDecimal.valueOf(-0.1), ReportedOriginContext.NORMAL_USE_WEAR, false, false, SafetyRiskLevel.SAFE_TO_RIDE));
        assertThrows(DomainValidationException.class, () ->
                policy.evaluateTriage(true, BigDecimal.valueOf(1.01), ReportedOriginContext.NORMAL_USE_WEAR, false, false, SafetyRiskLevel.SAFE_TO_RIDE));
        assertThrows(NullPointerException.class, () ->
                policy.evaluateTriage(true, null, ReportedOriginContext.NORMAL_USE_WEAR, false, false, SafetyRiskLevel.SAFE_TO_RIDE));
    }

    @Test
    @DisplayName("TC-AIR-16: Ineligible pre-existing damage context routes to HUMAN_TRIAGE")
    void shouldRouteToHumanTriageWhenPreExistingIneligible() {
        // SAFE_TO_RIDE -> LOW_AI_CONFIDENCE (weight 3)
        AiTriageDecision d1 = policy.evaluateTriage(
                true,
                BigDecimal.valueOf(0.95),
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false, // ineligible!
                false,
                SafetyRiskLevel.SAFE_TO_RIDE
        );
        assertEquals(InspectionStatus.HUMAN_TRIAGE, d1.targetStatus());
        assertEquals(QueuePriorityReason.LOW_AI_CONFIDENCE, d1.priorityReason());
        assertEquals(3, d1.priorityWeight());

        // CRITICAL_STOPPED -> CRITICAL_SAFETY (weight 1)
        AiTriageDecision d2 = policy.evaluateTriage(
                true,
                BigDecimal.valueOf(0.95),
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false, // ineligible!
                false,
                SafetyRiskLevel.CRITICAL_STOPPED
        );
        assertEquals(InspectionStatus.HUMAN_TRIAGE, d2.targetStatus());
        assertEquals(QueuePriorityReason.CRITICAL_SAFETY, d2.priorityReason());
        assertEquals(1, d2.priorityWeight());
    }
}
