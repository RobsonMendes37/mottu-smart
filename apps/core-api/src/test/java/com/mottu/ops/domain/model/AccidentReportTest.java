package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.VehicleDrivability;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.service.AccidentBudgetCalculator;
import com.mottu.ops.domain.service.AccidentStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccidentReportTest {

    private final AccidentBudgetCalculator calculator = new AccidentBudgetCalculator();
    private final AccidentStateMachine stateMachine = new AccidentStateMachine();

    @Test
    @DisplayName("UC-14.1: Criar relatório de acidente com moto DRIVABLE deve manter status e iniciar em QUEUED_AI")
    void shouldCreateInitialReportDrivable() {
        UUID contractId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID motorcycleId = UUID.randomUUID();

        AccidentReport report = AccidentReport.createInitial(
                "idempotency-key-1",
                contractId,
                riderId,
                motorcycleId,
                "accidents/2026/11/video.mp4",
                25,
                15000000L,
                List.of("accidents/2026/11/photo1.webp"),
                false,
                false,
                null,
                VehicleDrivability.DRIVABLE,
                1045,
                "Queda leve",
                "Sem obs"
        );

        assertThat(report.getId()).isNotNull();
        assertThat(report.getStatus()).isEqualTo(AccidentReportStatus.QUEUED_AI);
        assertThat(report.getDrivability()).isEqualTo(VehicleDrivability.DRIVABLE);
        assertThat(report.getVideoDurationSeconds()).isEqualTo(25);
        assertThat(report.getBudget().totalEstimated()).isEqualTo(Money.ZERO);
        assertThat(report.shouldLockMotorcycle()).isFalse();
    }

    @Test
    @DisplayName("UC-14.2: Criar relatório de acidente com moto NEEDS_TOWING deve indicar bloqueio de frota")
    void shouldCreateInitialReportNeedingTowing() {
        AccidentReport report = AccidentReport.createInitial(
                "idempotency-key-2",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "accidents/2026/11/video.mp4",
                30,
                20000000L,
                Collections.emptyList(),
                false,
                true,
                "BO-2026/999",
                VehicleDrivability.NEEDS_TOWING,
                1050,
                "Batida com outro veículo",
                null
        );

        assertThat(report.getStatus()).isEqualTo(AccidentReportStatus.QUEUED_AI);
        assertThat(report.getDrivability()).isEqualTo(VehicleDrivability.NEEDS_TOWING);
        assertThat(report.shouldLockMotorcycle()).isTrue();
        assertThat(report.getInvolvesThirdParty()).isTrue();
        assertThat(report.getPoliceReportNumber()).isEqualTo("BO-2026/999");
    }

    @Test
    @DisplayName("UC-14.3: Validar limites de vídeo: duração max 30s e tamanho max 30MB")
    void shouldValidateVideoConstraints() {
        assertThatThrownBy(() -> AccidentReport.createInitial(
                "key", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "video.mp4", 31, 1000L, Collections.emptyList(), false, false, null,
                VehicleDrivability.DRIVABLE, 1000, "Notes", null
        )).isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Video duration cannot exceed 30 seconds");

        assertThatThrownBy(() -> AccidentReport.createInitial(
                "key", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "video.mp4", 20, 35000000L, Collections.emptyList(), false, false, null,
                VehicleDrivability.DRIVABLE, 1000, "Notes", null
        )).isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Video size cannot exceed 30 MB");
    }

    @Test
    @DisplayName("UC-14.4: Aplicar estimativa da IA deve atualizar status para ESTIMATED_PENDING_WORKSHOP e calcular orçamento")
    void shouldApplyAiEstimation() {
        AccidentReport report = AccidentReport.createInitial(
                "idempotency-key-3",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "accidents/2026/11/video.mp4",
                20,
                10000000L,
                Collections.emptyList(),
                false,
                false,
                null,
                VehicleDrivability.NEEDS_TOWING,
                1045,
                "Queda",
                null
        );

        AccidentReportItem item1 = AccidentReportItem.createAiItem(UUID.randomUUID(), "Manete", Money.of(25.00), 1, 6);
        AccidentReportItem item2 = AccidentReportItem.createAiItem(UUID.randomUUID(), "Retrovisor", Money.of(42.50), 1, 14);

        report.applyAiEstimation(
                0.92,
                false,
                "Avarias no lado direito",
                List.of(item1, item2),
                calculator,
                stateMachine
        );

        assertThat(report.getStatus()).isEqualTo(AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP);
        assertThat(report.getAiConfidence()).isEqualTo(0.92);
        assertThat(report.getItems()).hasSize(2);
        // Subtotal = 67.50, mão de obra = 35.00, guincho = 80.00 -> Total = 182.50
        assertThat(report.getBudget().totalEstimated()).isEqualTo(Money.of(182.50));
    }

    @Test
    @DisplayName("UC-14.5: Marcar AWAITING_AI_RECOVERY em contingência de IA")
    void shouldMarkAwaitingAiRecovery() {
        AccidentReport report = AccidentReport.createInitial(
                "idempotency-key-4",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "accidents/2026/11/video.mp4",
                20,
                10000000L,
                Collections.emptyList(),
                false,
                false,
                null,
                VehicleDrivability.DRIVABLE,
                1045,
                "Queda",
                null
        );

        report.markAwaitingAiRecovery(stateMachine);
        assertThat(report.getStatus()).isEqualTo(AccidentReportStatus.AWAITING_AI_RECOVERY);
    }

    @Test
    @DisplayName("UC-14.6: Mecânico confirma laudo físico e atualiza orçamento para WORKSHOP_CONFIRMED")
    void shouldConfirmByMechanic() {
        AccidentReport report = AccidentReport.createInitial(
                "idempotency-key-5",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "accidents/2026/11/video.mp4",
                20,
                10000000L,
                Collections.emptyList(),
                false,
                false,
                null,
                VehicleDrivability.DRIVABLE,
                1045,
                "Queda",
                null
        );

        AccidentReportItem item1 = AccidentReportItem.createAiItem(UUID.randomUUID(), "Manete", Money.of(25.00), 1, 6);
        report.applyAiEstimation(0.95, false, "Resumo", List.of(item1), calculator, stateMachine);

        UUID mechanicId = UUID.randomUUID();
        AccidentReportItem extraItem = AccidentReportItem.createMechanicItem(UUID.randomUUID(), "Pedal", Money.of(80.00), 1);

        report.confirmByMechanic(
                mechanicId,
                List.of(item1),
                List.of(extraItem),
                "Confirmado na oficina com acréscimo do pedal",
                calculator,
                stateMachine
        );

        assertThat(report.getStatus()).isEqualTo(AccidentReportStatus.WORKSHOP_CONFIRMED);
        assertThat(report.getReviewedByMechanicId()).isEqualTo(mechanicId);
        assertThat(report.getMechanicNotes()).contains("Confirmado na oficina");
        assertThat(report.getConfirmedAt()).isNotNull();
        assertThat(report.getItems()).hasSize(2);
        // Subtotal = 25.00 + 80.00 = 105.00, Mão de obra = 35.00, Guincho = 0.00 -> 140.00
        assertThat(report.getBudget().totalEstimated()).isEqualTo(Money.of(140.00));
    }

    @Test
    @DisplayName("UC-14.7: Adicionar observação do entregador para o mecânico")
    void shouldUpdateRiderToMechanicNotes() {
        AccidentReport report = AccidentReport.createInitial(
                "idempotency-key-6",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "accidents/2026/11/video.mp4",
                20,
                10000000L,
                Collections.emptyList(),
                false,
                false,
                null,
                VehicleDrivability.DRIVABLE,
                1045,
                "Queda",
                null
        );

        report.updateRiderToMechanicNotes("A carenagem só ralou um pouco");
        assertThat(report.getRiderToMechanicNotes()).isEqualTo("A carenagem só ralou um pouco");
    }

    @Test
    @DisplayName("UC-14.8: Reconstituir relatório existente e exercitar getters")
    void shouldReconstituteAndExerciseAllGetters() {
        UUID id = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID motoId = UUID.randomUUID();
        UUID mechanicId = UUID.randomUUID();
        Instant now = Instant.now();

        AccidentReport report = AccidentReport.reconstitute(
                id,
                "idem-key",
                contractId,
                riderId,
                motoId,
                "video.mp4",
                20,
                15000000L,
                List.of("photo.webp"),
                true,
                false,
                null,
                VehicleDrivability.DRIVABLE,
                1100,
                "Notas relato",
                "Notas mecanico",
                AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP,
                0.90,
                true,
                "Resumo tecnico",
                AccidentBudget.zero(),
                Collections.emptyList(),
                mechanicId,
                "Parecer oficina",
                now,
                now
        );

        assertThat(report.getId()).isEqualTo(id);
        assertThat(report.getIdempotencyKey()).isEqualTo("idem-key");
        assertThat(report.getContractId()).isEqualTo(contractId);
        assertThat(report.getRiderId()).isEqualTo(riderId);
        assertThat(report.getMotorcycleId()).isEqualTo(motoId);
        assertThat(report.getVideoObjectKey()).isEqualTo("video.mp4");
        assertThat(report.getVideoSizeBytes()).isEqualTo(15000000L);
        assertThat(report.getPhotoObjectKeys()).containsExactly("photo.webp");
        assertThat(report.isHasInjuries()).isTrue();
        assertThat(report.isInvolvesThirdParty()).isFalse();
        assertThat(report.getInvolvesThirdParty()).isFalse();
        assertThat(report.getReportedOdometerKm()).isEqualTo(1100);
        assertThat(report.getRiderNotes()).isEqualTo("Notas relato");
        assertThat(report.isStructuralRiskAlert()).isTrue();
        assertThat(report.getAiTechnicalSummary()).isEqualTo("Resumo tecnico");
        assertThat(report.getCreatedAt()).isEqualTo(now);
    }
}

