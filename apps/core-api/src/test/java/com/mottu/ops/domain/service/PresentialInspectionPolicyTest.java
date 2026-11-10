package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresentialInspectionPolicyTest {

    private final PresentialInspectionPolicy policy = new PresentialInspectionPolicy();

    @Test
    @DisplayName("Convocação presencial deve impor custo zero imediato e motivo claro")
    void shouldEnforceZeroCostForPresentialInspection() {
        var result = policy.buildPresentialConvocatoria(
                "Ruído no motor e trepidação na embreagem impossíveis de julgar por foto"
        );

        assertThat(result.presentialRequested()).isTrue();
        assertThat(result.immediateCost()).isEqualTo(Money.ZERO);
        assertThat(result.recommendedVerdict()).isEqualTo(DamageVerdict.DESGASTE_NATURAL);
        assertThat(result.originLabel()).isEqualTo("DIAGNÓSTICO PRESENCIAL CONVOCADO");
        assertThat(result.reason()).contains("Ruído no motor");
    }

    @Test
    @DisplayName("Deve rejeitar motivo vazio para convocação presencial")
    void shouldRejectBlankReason() {
        assertThatThrownBy(() -> policy.buildPresentialConvocatoria(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
    }
}
