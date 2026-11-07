package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.model.AiTriageDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRecoveryPolicyTest {

    private AiRecoveryPolicy recoveryPolicy;

    @BeforeEach
    void setUp() {
        recoveryPolicy = new AiRecoveryPolicy();
    }

    @Test
    @DisplayName("TC-AIR-07: Retry 0 computes next retry at +30 seconds")
    void shouldComputeRetry0AtPlus30Seconds() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z");
        Instant next = recoveryPolicy.calculateNextRetry(now, 0);

        assertEquals(now.plusSeconds(30), next);
    }

    @Test
    @DisplayName("TC-AIR-08: Retry 1 computes next retry at +60 seconds")
    void shouldComputeRetry1AtPlus60Seconds() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z");
        Instant next = recoveryPolicy.calculateNextRetry(now, 1);

        assertEquals(now.plusSeconds(60), next);
    }

    @Test
    @DisplayName("TC-AIR-09: Retry 2 or higher computes next retry at +300 seconds (5 min)")
    void shouldComputeRetry2AtPlus300Seconds() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z");

        assertEquals(now.plusSeconds(300), recoveryPolicy.calculateNextRetry(now, 2));
        assertEquals(now.plusSeconds(300), recoveryPolicy.calculateNextRetry(now, 3));
    }

    @Test
    @DisplayName("TC-AIR-10: CRITICAL_STOPPED in recovery created 4m 59s ago remains in AWAITING_AI_RECOVERY")
    void shouldRemainAwaitingRecoveryBeforeSlaTimeout() {
        Instant now = Instant.parse("2026-11-01T12:05:00Z");
        Instant createdAt = now.minus(4, ChronoUnit.MINUTES).minus(59, ChronoUnit.SECONDS);

        AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                SafetyRiskLevel.CRITICAL_STOPPED,
                createdAt,
                now,
                1
        );

        assertEquals(InspectionStatus.AWAITING_AI_RECOVERY, decision.targetStatus());
        assertFalse(decision.requiresHumanTriage());
        assertNull(decision.priorityReason());
    }

    @Test
    @DisplayName("TC-AIR-11: CRITICAL_STOPPED in recovery created exactly 5m 00s ago escalates to HUMAN_TRIAGE with CRITICAL_SAFETY (weight 1)")
    void shouldEscalateAtExact5MinutesSlaForCriticalStopped() {
        Instant now = Instant.parse("2026-11-01T12:05:00Z");
        Instant createdAt = now.minus(5, ChronoUnit.MINUTES);

        AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                SafetyRiskLevel.CRITICAL_STOPPED,
                createdAt,
                now,
                1
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertTrue(decision.requiresHumanTriage());
        assertEquals(QueuePriorityReason.CRITICAL_SAFETY, decision.priorityReason());
        assertEquals(1, decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-12: CRITICAL_STOPPED in recovery created 6m 00s ago escalates to HUMAN_TRIAGE with CRITICAL_SAFETY")
    void shouldEscalateAfter5MinutesSlaForCriticalStopped() {
        Instant now = Instant.parse("2026-11-01T12:06:00Z");
        Instant createdAt = now.minus(6, ChronoUnit.MINUTES);

        AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                SafetyRiskLevel.CRITICAL_STOPPED,
                createdAt,
                now,
                2
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertEquals(QueuePriorityReason.CRITICAL_SAFETY, decision.priorityReason());
        assertEquals(1, decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-13: SAFE_TO_RIDE created 6m ago with retryCount=1 remains in recovery to prevent mechanic desk flood")
    void shouldKeepSafeToRideInRecoveryDuringRetries() {
        Instant now = Instant.parse("2026-11-01T12:06:00Z");
        Instant createdAt = now.minus(6, ChronoUnit.MINUTES);

        AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                SafetyRiskLevel.SAFE_TO_RIDE,
                createdAt,
                now,
                1
        );

        assertEquals(InspectionStatus.AWAITING_AI_RECOVERY, decision.targetStatus());
        assertFalse(decision.requiresHumanTriage());
    }

    @Test
    @DisplayName("TC-AIR-14: SAFE_TO_RIDE with retries exhausted (retryCount >= 3) escalates to HUMAN_TRIAGE with LOW_AI_CONFIDENCE (weight 3)")
    void shouldEscalateSafeToRideWhenRetriesExhausted() {
        Instant now = Instant.parse("2026-11-01T12:10:00Z");
        Instant createdAt = now.minus(10, ChronoUnit.MINUTES);

        AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                SafetyRiskLevel.SAFE_TO_RIDE,
                createdAt,
                now,
                3
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertTrue(decision.requiresHumanTriage());
        assertEquals(QueuePriorityReason.LOW_AI_CONFIDENCE, decision.priorityReason());
        assertEquals(3, decision.priorityWeight());
    }

    @Test
    @DisplayName("TC-AIR-15: CRITICAL_STOPPED with retries exhausted escalates to HUMAN_TRIAGE with CRITICAL_SAFETY (weight 1)")
    void shouldEscalateCriticalStoppedWhenRetriesExhausted() {
        Instant now = Instant.parse("2026-11-01T12:02:00Z");
        Instant createdAt = now.minus(2, ChronoUnit.MINUTES);

        AiTriageDecision decision = recoveryPolicy.evaluateRecoverySla(
                SafetyRiskLevel.CRITICAL_STOPPED,
                createdAt,
                now,
                3
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, decision.targetStatus());
        assertEquals(QueuePriorityReason.CRITICAL_SAFETY, decision.priorityReason());
        assertEquals(1, decision.priorityWeight());
    }

    @Test
    @DisplayName("Validation: Null parameters throw NullPointerException")
    void shouldThrowOnNullParameters() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () -> recoveryPolicy.calculateNextRetry(null, 0));
        assertThrows(NullPointerException.class, () -> recoveryPolicy.evaluateRecoverySla(null, now, now, 0));
        assertThrows(NullPointerException.class, () -> recoveryPolicy.evaluateRecoverySla(SafetyRiskLevel.SAFE_TO_RIDE, null, now, 0));
        assertThrows(NullPointerException.class, () -> recoveryPolicy.evaluateRecoverySla(SafetyRiskLevel.SAFE_TO_RIDE, now, null, 0));
    }
}
