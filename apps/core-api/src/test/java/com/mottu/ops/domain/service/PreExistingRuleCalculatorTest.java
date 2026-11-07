package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.PreExistingDamageRecord;
import com.mottu.ops.domain.model.PreExistingEvaluationResult;
import com.mottu.ops.domain.model.RentalContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreExistingRuleCalculatorTest {

    private PreExistingRuleCalculator calculator;
    private Instant pickupTime;
    private RentalContract contract;

    @BeforeEach
    void setUp() {
        calculator = new PreExistingRuleCalculator();
        pickupTime = Instant.parse("2026-11-01T10:00:00Z");
        contract = new RentalContract(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                pickupTime,
                1000,
                null,
                true
        );
    }

    @Test
    @DisplayName("UC-05 Test 1: Exactly 48.0h and 150 km (hours <= 48h is true) -> ELIGIBLE")
    void shouldBeEligibleWhenHoursExactly48EvenIfKmExceeded() {
        Instant inspectionTime = pickupTime.plus(48, ChronoUnit.HOURS);
        int reportedOdometer = 1150; // 150 km driven

        boolean eligible = calculator.isEligibleByWindow(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false
        );

        assertTrue(eligible, "Should be eligible when hours <= 48.0 even if km > 100");
    }

    @Test
    @DisplayName("UC-05 Test 2: 72.0h and exactly 100 km (km <= 100 is true) -> ELIGIBLE")
    void shouldBeEligibleWhenKmExactly100EvenIfHoursExceeded() {
        Instant inspectionTime = pickupTime.plus(72, ChronoUnit.HOURS);
        int reportedOdometer = 1100; // exactly 100 km driven

        boolean eligible = calculator.isEligibleByWindow(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false
        );

        assertTrue(eligible, "Should be eligible when km <= 100 even if hours > 48.0");
    }

    @Test
    @DisplayName("UC-05 Test 3: 48.1h (48h 6min) AND 101 km (both exceeded) -> INELIGIBLE")
    void shouldBeIneligibleWhenBothHoursAndKmExceeded() {
        Instant inspectionTime = pickupTime.plus(48, ChronoUnit.HOURS).plus(6, ChronoUnit.MINUTES);
        int reportedOdometer = 1101; // 101 km driven

        boolean eligible = calculator.isEligibleByWindow(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false
        );

        assertFalse(eligible, "Should be ineligible when both hours > 48.0 and km > 100");
    }

    @Test
    @DisplayName("Boundary: Exactly 48.0h and 100 km (both boundaries met) -> ELIGIBLE")
    void shouldBeEligibleAtExactDoubleBoundary() {
        Instant inspectionTime = pickupTime.plus(48, ChronoUnit.HOURS);
        int reportedOdometer = 1100;

        boolean eligible = calculator.isEligibleByWindow(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false
        );

        assertTrue(eligible);
    }

    @Test
    @DisplayName("Fraud suspected -> Immediately INELIGIBLE regardless of time and distance")
    void shouldBeIneligibleWhenFraudSuspected() {
        Instant inspectionTime = pickupTime.plus(1, ChronoUnit.HOURS);
        int reportedOdometer = 1005;

        boolean eligible = calculator.isEligibleByWindow(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                true // fraud!
        );

        assertFalse(eligible);
    }

    @Test
    @DisplayName("Context != RECEIVED_LIKE_THIS -> INELIGIBLE for pre-existing exemption")
    void shouldBeIneligibleWhenContextIsNotReceivedLikeThis() {
        Instant inspectionTime = pickupTime.plus(1, ChronoUnit.HOURS);
        int reportedOdometer = 1005;

        assertFalse(calculator.isEligibleByWindow(contract, reportedOdometer, inspectionTime, ReportedOriginContext.NORMAL_USE_WEAR, false));
        assertFalse(calculator.isEligibleByWindow(contract, reportedOdometer, inspectionTime, ReportedOriginContext.ACCIDENT_OR_DROP, false));
    }

    @Test
    @DisplayName("Validation: Reported odometer less than initial odometer throws DomainValidationException")
    void shouldThrowWhenReportedOdometerLessThanInitial() {
        Instant inspectionTime = pickupTime.plus(1, ChronoUnit.HOURS);
        int invalidOdometer = 950; // less than 1000

        assertThrows(DomainValidationException.class, () ->
                calculator.isEligibleByWindow(contract, invalidOdometer, inspectionTime, ReportedOriginContext.RECEIVED_LIKE_THIS, false)
        );
    }

    @Test
    @DisplayName("Validation: Inspection timestamp before pickup throws DomainValidationException")
    void shouldThrowWhenInspectionTimestampBeforePickup() {
        Instant invalidTime = pickupTime.minus(1, ChronoUnit.HOURS);

        assertThrows(DomainValidationException.class, () ->
                calculator.isEligibleByWindow(contract, 1050, invalidTime, ReportedOriginContext.RECEIVED_LIKE_THIS, false)
        );
    }

    @Test
    @DisplayName("Evaluate with matching baseline damage -> EXEMPT even if window exceeded")
    void shouldBeExemptWhenMatchingBaselineDamageFound() {
        Instant inspectionTime = pickupTime.plus(100, ChronoUnit.HOURS);
        int reportedOdometer = 1500; // 500 km
        List<PreExistingDamageRecord> baseline = List.of(
                new PreExistingDamageRecord("Retrovisor Esquerdo", "Riscado no galpão", pickupTime, "a".repeat(64))
        );

        PreExistingEvaluationResult result = calculator.evaluate(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false,
                baseline,
                "retrovisor esquerdo"
        );

        assertTrue(result.eligibleForExemption());
        assertTrue(result.explanation().contains("baseline"));
    }

    @Test
    @DisplayName("Evaluate with no baseline match and window exceeded -> NOT EXEMPT")
    void shouldNotBeExemptWhenWindowExceededAndNoBaselineMatch() {
        Instant inspectionTime = pickupTime.plus(50, ChronoUnit.HOURS);
        int reportedOdometer = 1200; // 200 km
        List<PreExistingDamageRecord> baseline = List.of();

        PreExistingEvaluationResult result = calculator.evaluate(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false,
                baseline,
                "Manete de Freio"
        );

        assertFalse(result.eligibleForExemption());
    }

    @Test
    @DisplayName("Evaluate with no baseline match but within window -> EXEMPT")
    void shouldBeExemptWhenNoBaselineMatchButWithinWindow() {
        Instant inspectionTime = pickupTime.plus(2, ChronoUnit.HOURS);
        int reportedOdometer = 1020;

        PreExistingEvaluationResult res1 = calculator.evaluate(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false,
                null,
                null
        );
        assertTrue(res1.eligibleForExemption());

        List<PreExistingDamageRecord> nonMatching = List.of(
                new PreExistingDamageRecord("Retrovisor", "desc", pickupTime, "a".repeat(64))
        );
        PreExistingEvaluationResult res2 = calculator.evaluate(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                false,
                nonMatching,
                "Farol"
        );
        assertTrue(res2.eligibleForExemption());
    }

    @Test
    @DisplayName("Evaluate with fraud suspected -> NOT EXEMPT")
    void shouldNotBeExemptWhenFraudSuspectedInEvaluate() {
        Instant inspectionTime = pickupTime.plus(1, ChronoUnit.HOURS);
        int reportedOdometer = 1010;
        List<PreExistingDamageRecord> baseline = List.of(
                new PreExistingDamageRecord("Retrovisor Esquerdo", "Riscado", pickupTime, "a".repeat(64))
        );

        PreExistingEvaluationResult result = calculator.evaluate(
                contract,
                reportedOdometer,
                inspectionTime,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                true,
                baseline,
                "Retrovisor Esquerdo"
        );

        assertFalse(result.eligibleForExemption());
        assertTrue(result.explanation().contains("fraud"));
    }

    @Test
    @DisplayName("Evaluate with null contract throws NullPointerException")
    void shouldThrowWhenContractIsNull() {
        assertThrows(NullPointerException.class, () ->
                calculator.isEligibleByWindow(null, 1050, pickupTime, ReportedOriginContext.RECEIVED_LIKE_THIS, false)
        );
    }

    @Test
    @DisplayName("Evaluate with null inspection timestamp throws NullPointerException")
    void shouldThrowWhenInspectionTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                calculator.isEligibleByWindow(contract, 1050, null, ReportedOriginContext.RECEIVED_LIKE_THIS, false)
        );
    }

    @Test
    @DisplayName("Evaluate with null reported context throws NullPointerException")
    void shouldThrowWhenReportedContextIsNull() {
        assertThrows(NullPointerException.class, () ->
                calculator.isEligibleByWindow(contract, 1050, pickupTime, null, false)
        );
    }
}
