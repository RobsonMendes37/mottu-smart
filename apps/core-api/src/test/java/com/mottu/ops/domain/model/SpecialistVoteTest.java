package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecialistVoteTest {

    @Test
    @DisplayName("Deve criar voto de especialista válido")
    void shouldCreateValidVote() {
        SpecialistVote vote = new SpecialistVote(
                "PERITO_DESGASTE",
                DamageVerdict.DESGASTE_NATURAL,
                0.95,
                "Banda de rodagem compatível com alta quilometragem"
        );

        assertThat(vote.specialistRole()).isEqualTo("PERITO_DESGASTE");
        assertThat(vote.verdict()).isEqualTo(DamageVerdict.DESGASTE_NATURAL);
        assertThat(vote.confidence()).isEqualTo(0.95);
        assertThat(vote.reasoning()).contains("alta quilometragem");
    }

    @Test
    @DisplayName("Deve lançar exceção se papel for nulo ou em branco")
    void shouldThrowWhenRoleIsBlank() {
        assertThatThrownBy(() -> new SpecialistVote(
                null,
                DamageVerdict.DESGASTE_NATURAL,
                0.90,
                "Justificativa"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("papel do perito");

        assertThatThrownBy(() -> new SpecialistVote(
                "   ",
                DamageVerdict.DESGASTE_NATURAL,
                0.90,
                "Justificativa"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("papel do perito");
    }

    @Test
    @DisplayName("Deve lançar exceção se veredito for nulo")
    void shouldThrowWhenVerdictIsNull() {
        assertThatThrownBy(() -> new SpecialistVote(
                "PERITO_DESGASTE",
                null,
                0.90,
                "Justificativa"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("veredito do perito");
    }
}

