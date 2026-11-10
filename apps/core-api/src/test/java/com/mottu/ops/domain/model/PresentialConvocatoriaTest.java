package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresentialConvocatoriaTest {

    @Test
    @DisplayName("Deve instanciar convocação presencial válida")
    void shouldCreateValidPresentialConvocatoria() {
        PresentialConvocatoria conv = new PresentialConvocatoria(
                true,
                Money.of(new BigDecimal("150.00")),
                DamageVerdict.MAU_USO,
                "Oficina Butantã",
                "Dano estrutural detectado"
        );

        assertThat(conv.presentialRequested()).isTrue();
        assertThat(conv.immediateCost().getAmount()).isEqualByComparingTo("150.00");
        assertThat(conv.recommendedVerdict()).isEqualTo(DamageVerdict.MAU_USO);
        assertThat(conv.originLabel()).isEqualTo("Oficina Butantã");
        assertThat(conv.reason()).isEqualTo("Dano estrutural detectado");
    }

    @Test
    @DisplayName("Deve falhar se custo imediato for nulo")
    void shouldThrowWhenImmediateCostIsNull() {
        assertThatThrownBy(() -> new PresentialConvocatoria(
                true,
                null,
                DamageVerdict.MAU_USO,
                "Oficina",
                "Motivo"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("custo imediato");
    }

    @Test
    @DisplayName("Deve falhar se motivo for nulo ou em branco")
    void shouldThrowWhenReasonIsBlank() {
        assertThatThrownBy(() -> new PresentialConvocatoria(
                true,
                Money.of(BigDecimal.ZERO),
                DamageVerdict.MAU_USO,
                "Oficina",
                "   "
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("motivo da convocação presencial");

        assertThatThrownBy(() -> new PresentialConvocatoria(
                true,
                Money.of(BigDecimal.ZERO),
                DamageVerdict.MAU_USO,
                "Oficina",
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("motivo da convocação presencial");
    }
}

