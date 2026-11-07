package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectionTest {

    @Test
    @DisplayName("Normal creation creates QUEUED_AI inspection with null priority")
    void shouldCreateNormalInspection() {
        UUID contractId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID motorcycleId = UUID.randomUUID();
        Instant now = Instant.now();

        Inspection inspection = Inspection.createInitial(
                "idemp-123",
                contractId,
                riderId,
                motorcycleId,
                "photos/item.webp",
                "a".repeat(64),
                "0123456789abcdef",
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1050,
                "Manete torto",
                now
        );

        assertNotNull(inspection.getId());
        assertEquals("idemp-123", inspection.getIdempotencyKey());
        assertEquals(contractId, inspection.getContractId());
        assertEquals(riderId, inspection.getRiderId());
        assertEquals(motorcycleId, inspection.getMotorcycleId());
        assertEquals("photos/item.webp", inspection.getPhotoObjectKey());
        assertEquals("a".repeat(64), inspection.getPhotoSha256());
        assertEquals("0123456789abcdef", inspection.getPhotoPerceptualHash());
        assertEquals(InspectionStatus.QUEUED_AI, inspection.getStatus());
        assertNull(inspection.getPriorityReason());
        assertNull(inspection.getPriorityWeight());
        assertFalse(inspection.isFraudSuspected());
        assertNull(inspection.getFraudReason());
        assertEquals(ReportedOriginContext.RECEIVED_LIKE_THIS, inspection.getReportedContext());
        assertEquals(SafetyRiskLevel.SAFE_TO_RIDE, inspection.getSafetyRisk());
        assertEquals(1050, inspection.getReportedOdometerKm());
        assertEquals("Manete torto", inspection.getRiderNotes());
        assertEquals(now, inspection.getCreatedAt());
        assertNull(inspection.getResolvedAt());
        assertNull(inspection.getAiConfidence());
        assertNull(inspection.getAiVerdict());
        assertNull(inspection.getAiTechnicalSummary());
        assertNull(inspection.getFinalVerdict());
        assertEquals(Money.ZERO, inspection.getLaborFee());
        assertEquals(Money.ZERO, inspection.getTotalEstimated());
        assertNull(inspection.getCustomerFeedbackText());
        assertFalse(inspection.isContestedOnce());
        assertNull(inspection.getContestationReason());
        assertNull(inspection.getReviewedByMechanicId());
        assertNull(inspection.getMechanicNotes());
        assertEquals(0, inspection.getRetryCount());
        assertNull(inspection.getNextRetryAt());
        assertTrue(inspection.getItems().isEmpty());
    }

    @Test
    @DisplayName("Creation with fraudSuspected=true creates HUMAN_TRIAGE with FRAUD_SUSPICION (weight 4)")
    void shouldCreateFraudSuspectedInspection() {
        Inspection inspection = Inspection.createInitial(
                "idemp-fraud-1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/fraud.webp",
                "b".repeat(64),
                "ffffffffffffffff",
                true,
                "PHOTO_FRAUD_DETECTED: SHA-256 match",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1050,
                null,
                Instant.now()
        );

        assertEquals(InspectionStatus.HUMAN_TRIAGE, inspection.getStatus());
        assertEquals(QueuePriorityReason.FRAUD_SUSPICION, inspection.getPriorityReason());
        assertEquals(4, inspection.getPriorityWeight());
        assertTrue(inspection.isFraudSuspected());
        assertEquals("PHOTO_FRAUD_DETECTED: SHA-256 match", inspection.getFraudReason());
    }

    @Test
    @DisplayName("flagFraud sets HUMAN_TRIAGE and FRAUD_SUSPICION weight 4")
    void shouldFlagFraudOnExistingInspection() {
        Inspection inspection = Inspection.createInitial(
                "idemp-test",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/test.webp",
                "c".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        inspection.flagFraud("PHOTO_FRAUD_DETECTED: dHash match");

        assertTrue(inspection.isFraudSuspected());
        assertEquals("PHOTO_FRAUD_DETECTED: dHash match", inspection.getFraudReason());
        assertEquals(InspectionStatus.HUMAN_TRIAGE, inspection.getStatus());
        assertEquals(QueuePriorityReason.FRAUD_SUSPICION, inspection.getPriorityReason());
        assertEquals(4, inspection.getPriorityWeight());
    }

    @Test
    @DisplayName("applyPreExistingExemption sets PRE_EXISTENTE verdict and R$ 0.00 costs")
    void shouldApplyPreExistingExemption() {
        Inspection inspection = Inspection.createInitial(
                "idemp-exempt",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/exempt.webp",
                "d".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        inspection.applyPreExistingExemption();

        assertEquals(DamageVerdict.PRE_EXISTENTE, inspection.getFinalVerdict());
        assertEquals(Money.ZERO, inspection.getLaborFee());
        assertEquals(Money.ZERO, inspection.getTotalEstimated());
    }

    @Test
    @DisplayName("applyPreExistingExemption on fraudulent inspection throws BusinessRuleViolationException")
    void shouldThrowWhenApplyingExemptionToFraud() {
        Inspection inspection = Inspection.createInitial(
                "idemp-fraud-exempt",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/fraud.webp",
                "e".repeat(64),
                null,
                true,
                "PHOTO_FRAUD_DETECTED",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        assertThrows(BusinessRuleViolationException.class, inspection::applyPreExistingExemption);
    }

    @Test
    @DisplayName("autoResolve updates verdict, confidence, costs, and sets AUTO_RESOLVED")
    void shouldAutoResolveInspection() {
        Inspection inspection = Inspection.createInitial(
                "idemp-auto",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/auto.webp",
                "1".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        Instant resolvedTime = Instant.now();
        inspection.autoResolve(
                DamageVerdict.MAU_USO,
                BigDecimal.valueOf(0.92),
                Money.of(35.0),
                Money.of(75.0),
                "Damage confirmed by AI",
                resolvedTime
        );

        assertEquals(InspectionStatus.AUTO_RESOLVED, inspection.getStatus());
        assertEquals(DamageVerdict.MAU_USO, inspection.getAiVerdict());
        assertEquals(DamageVerdict.MAU_USO, inspection.getFinalVerdict());
        assertEquals(BigDecimal.valueOf(0.92), inspection.getAiConfidence());
        assertEquals(Money.of(35.0), inspection.getLaborFee());
        assertEquals(Money.of(75.0), inspection.getTotalEstimated());
        assertEquals("Damage confirmed by AI", inspection.getAiTechnicalSummary());
        assertEquals(resolvedTime, inspection.getResolvedAt());
    }

    @Test
    @DisplayName("autoResolve on fraud throws BusinessRuleViolationException")
    void shouldThrowWhenAutoResolvingFraud() {
        Inspection inspection = Inspection.createInitial(
                "idemp-auto-fraud",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/auto.webp",
                "1".repeat(64),
                null,
                true,
                "fraud",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        assertThrows(BusinessRuleViolationException.class, () ->
                inspection.autoResolve(DamageVerdict.MAU_USO, BigDecimal.valueOf(0.9), Money.ZERO, Money.ZERO, "desc", Instant.now())
        );
    }

    @Test
    @DisplayName("routeToHumanTriage assigns priority reason and weight, with CRITICAL_STOPPED having weight 1")
    void shouldRouteToHumanTriage() {
        Inspection safeInspection = Inspection.createInitial(
                "idemp-safe",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/safe.webp",
                "2".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        safeInspection.routeToHumanTriage(QueuePriorityReason.LOW_AI_CONFIDENCE);
        assertEquals(InspectionStatus.HUMAN_TRIAGE, safeInspection.getStatus());
        assertEquals(QueuePriorityReason.LOW_AI_CONFIDENCE, safeInspection.getPriorityReason());
        assertEquals(3, safeInspection.getPriorityWeight());

        Inspection stoppedInspection = Inspection.createInitial(
                "idemp-stopped",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/stopped.webp",
                "3".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.CRITICAL_STOPPED,
                1000,
                null,
                Instant.now()
        );

        stoppedInspection.routeToHumanTriage(QueuePriorityReason.LOW_AI_CONFIDENCE);
        assertEquals(InspectionStatus.HUMAN_TRIAGE, stoppedInspection.getStatus());
        assertEquals(1, stoppedInspection.getPriorityWeight()); // Critical stopped forces weight 1
    }

    @Test
    @DisplayName("Contest valid inspection moves to HUMAN_TRIAGE with RIDER_CONTESTATION weight 2")
    void shouldContestAutoResolvedInspection() {
        Inspection inspection = Inspection.createInitial(
                "idemp-contest",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/contest.webp",
                "f".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );
        inspection.autoResolve(DamageVerdict.MAU_USO, BigDecimal.valueOf(0.85), Money.of(35.0), Money.of(60.0), "Laudo", Instant.now());

        inspection.contest("Eu recebi a moto exatamente assim ontem cedo.");

        assertTrue(inspection.isContestedOnce());
        assertEquals("Eu recebi a moto exatamente assim ontem cedo.", inspection.getContestationReason());
        assertEquals(InspectionStatus.HUMAN_TRIAGE, inspection.getStatus());
        assertEquals(QueuePriorityReason.RIDER_CONTESTATION, inspection.getPriorityReason());
        assertEquals(2, inspection.getPriorityWeight());
    }

    @Test
    @DisplayName("Contest on CRITICAL_STOPPED inspection sets priority weight to 1")
    void shouldContestCriticalStoppedInspection() {
        Inspection inspection = Inspection.createInitial(
                "idemp-contest-crit",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/contest.webp",
                "f".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.CRITICAL_STOPPED,
                1000,
                null,
                Instant.now()
        );
        inspection.autoResolve(DamageVerdict.MAU_USO, BigDecimal.valueOf(0.85), Money.of(35.0), Money.of(60.0), "Laudo", Instant.now());

        inspection.contest("Justificativa válida com mais de dez caracteres");
        assertEquals(1, inspection.getPriorityWeight());
    }

    @Test
    @DisplayName("Contest when not AUTO_RESOLVED throws BusinessRuleViolationException")
    void shouldThrowWhenContestingNotAutoResolved() {
        Inspection inspection = Inspection.createInitial(
                "idemp-contest-err",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/err.webp",
                "4".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        assertThrows(BusinessRuleViolationException.class, () -> inspection.contest("Justificativa com mais de dez caracteres"));
    }

    @Test
    @DisplayName("Contest when already contested once throws BusinessRuleViolationException")
    void shouldThrowWhenContestingTwice() {
        Inspection inspection = Inspection.createInitial(
                "idemp-contest-twice",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/contest.webp",
                "5".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );
        inspection.autoResolve(DamageVerdict.MAU_USO, BigDecimal.valueOf(0.85), Money.of(35.0), Money.of(60.0), "Laudo", Instant.now());
        inspection.contest("Primeira contestação válida");

        // Try contesting again
        assertThrows(BusinessRuleViolationException.class, () -> inspection.contest("Segunda tentativa de contestação"));
    }

    @Test
    @DisplayName("Contest with short justification (<10 chars) or null throws DomainValidationException")
    void shouldThrowOnShortContestationReason() {
        Inspection inspection = Inspection.createInitial(
                "idemp-short",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/short.webp",
                "6".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );
        inspection.autoResolve(DamageVerdict.MAU_USO, BigDecimal.valueOf(0.85), Money.of(35.0), Money.of(60.0), "Laudo", Instant.now());

        assertThrows(DomainValidationException.class, () -> inspection.contest("não fui"));
        assertThrows(DomainValidationException.class, () -> inspection.contest(null));
        assertThrows(DomainValidationException.class, () -> inspection.contest("   12345   "));
    }

    @Test
    @DisplayName("resolveByMechanic resolves inspection in HUMAN_TRIAGE")
    void shouldResolveByMechanic() {
        Inspection inspection = Inspection.createInitial(
                "idemp-mech",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/mech.webp",
                "7".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );
        inspection.routeToHumanTriage(QueuePriorityReason.LOW_AI_CONFIDENCE);

        UUID mechanicId = UUID.randomUUID();
        Instant resolvedTime = Instant.now();
        inspection.resolveByMechanic(
                mechanicId,
                DamageVerdict.DESGASTE_NATURAL,
                Money.ZERO,
                Money.ZERO,
                "Desgaste normal de corrente",
                "Aprovado sem custos",
                resolvedTime
        );

        assertEquals(InspectionStatus.HUMAN_RESOLVED, inspection.getStatus());
        assertEquals(mechanicId, inspection.getReviewedByMechanicId());
        assertEquals(DamageVerdict.DESGASTE_NATURAL, inspection.getFinalVerdict());
        assertEquals(Money.ZERO, inspection.getLaborFee());
        assertEquals(Money.ZERO, inspection.getTotalEstimated());
        assertEquals("Desgaste normal de corrente", inspection.getMechanicNotes());
        assertEquals("Aprovado sem custos", inspection.getCustomerFeedbackText());
        assertEquals(resolvedTime, inspection.getResolvedAt());
    }

    @Test
    @DisplayName("resolveByMechanic when status is not HUMAN_TRIAGE throws BusinessRuleViolationException")
    void shouldThrowWhenResolvingByMechanicOutsideTriage() {
        Inspection inspection = Inspection.createInitial(
                "idemp-mech-err",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/mech.webp",
                "8".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        assertThrows(BusinessRuleViolationException.class, () ->
                inspection.resolveByMechanic(UUID.randomUUID(), DamageVerdict.MAU_USO, Money.ZERO, Money.ZERO, "notes", "feedback", null)
        );
    }

    @Test
    @DisplayName("addItem adds inspection item to list")
    void shouldAddInspectionItem() {
        Inspection inspection = Inspection.createInitial(
                "idemp-item",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photos/item.webp",
                "9".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1000,
                null,
                Instant.now()
        );

        InspectionItem item = new InspectionItem(
                UUID.randomUUID(),
                inspection.getId(),
                UUID.randomUUID(),
                "Manete de Freio",
                Money.of(25.0),
                "AI"
        );
        inspection.addItem(item);

        assertEquals(1, inspection.getItems().size());
        assertEquals(item, inspection.getItems().get(0));

        assertThrows(NullPointerException.class, () -> inspection.addItem(null));
    }

    @Test
    @DisplayName("Constructor validations throw DomainValidationException")
    void shouldValidateConstructorParameters() {
        UUID validId = UUID.randomUUID();
        String validIdemp = "idemp";
        String validKey = "key";
        String validSha = "a".repeat(64);
        ReportedOriginContext validCtx = ReportedOriginContext.RECEIVED_LIKE_THIS;
        SafetyRiskLevel validRisk = SafetyRiskLevel.SAFE_TO_RIDE;
        Instant validNow = Instant.now();

        // null or blank idempotencyKey
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, null, validId, validId, validId, validKey, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, "  ", validId, validId, validId, validKey, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));

        // null contractId, riderId, motorcycleId
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, null, validId, validId, validKey, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, null, validId, validKey, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, null, validKey, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));

        // null or blank photoObjectKey
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, null, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, "   ", validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));

        // invalid sha256 length
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, null, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, "short", null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));

        // null context, null risk, negative odometer
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, validSha, null, false, null, null, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, validSha, null, false, null, validCtx, null, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, validSha, null, false, null, validCtx, validRisk, -1, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));

        // rider notes > 300 chars
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, validSha, null, false, null, validCtx, validRisk, 100, "x".repeat(301), null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, validNow, null, null));

        // null createdAt
        assertThrows(DomainValidationException.class, () ->
                new Inspection(validId, validIdemp, validId, validId, validId, validKey, validSha, null, false, null, validCtx, validRisk, 100, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, 0, null, null, null, null));
    }

    @Test
    @DisplayName("Exercise constructor and methods defaults and null-handling branches")
    void shouldExerciseConstructorAndMethodDefaults() {
        Inspection initialWithNullCreated = Inspection.createInitial(
                "idemp-null-created",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "photo.webp",
                "a".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                100,
                null,
                null // null createdAt triggers Instant.now()
        );
        assertNotNull(initialWithNullCreated.getCreatedAt());

        initialWithNullCreated.autoResolve(
                DamageVerdict.MAU_USO,
                BigDecimal.valueOf(0.9),
                null, // null laborFee triggers Money.ZERO
                null, // null total triggers Money.ZERO
                "summary",
                Instant.now()
        );
        assertEquals(Money.ZERO, initialWithNullCreated.getLaborFee());
        assertEquals(Money.ZERO, initialWithNullCreated.getTotalEstimated());

        // Full constructor with explicit non-null values
        UUID id = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID motorcycleId = UUID.randomUUID();
        UUID mechanicId = UUID.randomUUID();
        Instant now = Instant.now();
        InspectionItem item = new InspectionItem(UUID.randomUUID(), id, UUID.randomUUID(), "Part", Money.of(10), "AI");

        Inspection full = new Inspection(
                id,
                "idemp-full",
                contractId,
                riderId,
                motorcycleId,
                "key",
                "b".repeat(64),
                "hash",
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                200,
                "notes",
                InspectionStatus.HUMAN_TRIAGE,
                3,
                QueuePriorityReason.LOW_AI_CONFIDENCE,
                BigDecimal.valueOf(0.75),
                DamageVerdict.MAU_USO,
                "tech",
                DamageVerdict.MAU_USO,
                Money.of(35),
                Money.of(45),
                "feedback",
                true,
                "contest reason",
                mechanicId,
                "mech notes",
                1,
                now.plusSeconds(30),
                now,
                now,
                List.of(item)
        );
        assertEquals(id, full.getId());
        assertEquals(1, full.getItems().size());
        assertEquals(1, full.getRetryCount());
        assertEquals(now.plusSeconds(30), full.getNextRetryAt());

        // resolveByMechanic with null resolvedAt triggers Instant.now()
        Inspection triage = new Inspection(
                null,
                "idemp-triage",
                contractId,
                riderId,
                motorcycleId,
                "key",
                "c".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                200,
                null,
                InspectionStatus.HUMAN_TRIAGE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                0,
                null,
                now,
                null,
                null
        );
        triage.resolveByMechanic(mechanicId, DamageVerdict.MAU_USO, Money.of(35), Money.of(35), "notes", "fb", null);
        assertNotNull(triage.getResolvedAt());

        // Constructor with null status and fraudSuspected=true
        Inspection fraudNullStatus = new Inspection(
                null,
                "idemp-fraud-null",
                contractId,
                riderId,
                motorcycleId,
                "key",
                "d".repeat(64),
                null,
                true,
                "fraud reason",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                100,
                null,
                null, // status is null -> HUMAN_TRIAGE
                null, // weight is null -> 4
                null, // reason is null -> FRAUD_SUSPICION
                null, null, null, null,
                null, null, null, false, null, null, null, 0, null, now, null, null
        );
        assertEquals(InspectionStatus.HUMAN_TRIAGE, fraudNullStatus.getStatus());
        assertEquals(QueuePriorityReason.FRAUD_SUSPICION, fraudNullStatus.getPriorityReason());
        assertEquals(4, fraudNullStatus.getPriorityWeight());

        // Constructor with null status and fraudSuspected=false
        Inspection cleanNullStatus = new Inspection(
                null,
                "idemp-clean-null",
                contractId,
                riderId,
                motorcycleId,
                "key",
                "e".repeat(64),
                null,
                false,
                null,
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                100,
                null,
                null, // status is null -> QUEUED_AI
                null, // weight is null -> null
                null, // reason is null -> null
                null, null, null, null,
                null, null, null, false, null, null, null, 0, null, now, null, null
        );
        assertEquals(InspectionStatus.QUEUED_AI, cleanNullStatus.getStatus());
        assertNull(cleanNullStatus.getPriorityReason());
        assertNull(cleanNullStatus.getPriorityWeight());
    }

    @Test
    @DisplayName("scheduleAppointment transitions from AUTO_RESOLVED or HUMAN_RESOLVED to SCHEDULED")
    void shouldTransitionToScheduledWhenResolved() {
        UUID contractId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID motorcycleId = UUID.randomUUID();
        Instant now = Instant.now();

        // 1. From AUTO_RESOLVED
        Inspection autoResolved = new Inspection(
                UUID.randomUUID(), "idemp-sched-1", contractId, riderId, motorcycleId,
                "key", "a".repeat(64), null, false, null,
                ReportedOriginContext.RECEIVED_LIKE_THIS, SafetyRiskLevel.SAFE_TO_RIDE, 100, null,
                InspectionStatus.AUTO_RESOLVED, null, null, null, null, null, null,
                null, null, null, false, null, null, null, 0, null, now, now, null
        );
        autoResolved.scheduleAppointment();
        assertEquals(InspectionStatus.SCHEDULED, autoResolved.getStatus());

        // 2. From HUMAN_RESOLVED
        Inspection humanResolved = new Inspection(
                UUID.randomUUID(), "idemp-sched-2", contractId, riderId, motorcycleId,
                "key", "b".repeat(64), null, false, null,
                ReportedOriginContext.RECEIVED_LIKE_THIS, SafetyRiskLevel.SAFE_TO_RIDE, 100, null,
                InspectionStatus.HUMAN_RESOLVED, null, null, null, null, null, null,
                null, null, null, false, null, null, null, 0, null, now, now, null
        );
        humanResolved.scheduleAppointment();
        assertEquals(InspectionStatus.SCHEDULED, humanResolved.getStatus());

        // 3. From QUEUED_AI should fail
        Inspection queued = new Inspection(
                UUID.randomUUID(), "idemp-sched-3", contractId, riderId, motorcycleId,
                "key", "c".repeat(64), null, false, null,
                ReportedOriginContext.RECEIVED_LIKE_THIS, SafetyRiskLevel.SAFE_TO_RIDE, 100, null,
                InspectionStatus.QUEUED_AI, null, null, null, null, null, null,
                null, null, null, false, null, null, null, 0, null, now, null, null
        );
        assertThrows(BusinessRuleViolationException.class, queued::scheduleAppointment);
    }
}

