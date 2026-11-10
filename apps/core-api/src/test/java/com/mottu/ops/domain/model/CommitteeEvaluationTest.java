package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommitteeEvaluationTest {

    @Test
    @DisplayName("Deve criar deliberação válida do comitê com lista de votos")
    void shouldCreateValidCommitteeEvaluation() {
        SpecialistVote vote1 = new SpecialistVote("PERITO_DESGASTE", DamageVerdict.DESGASTE_NATURAL, 0.9, "Desgaste");
        SpecialistVote vote2 = new SpecialistVote("PERITO_IMPACTO", DamageVerdict.DESGASTE_NATURAL, 0.85, "Sem trinca");
        CommitteeEvaluation eval = new CommitteeEvaluation(
                DamageVerdict.DESGASTE_NATURAL,
                "2/2 Unânime",
                true,
                true,
                false,
                0.875,
                "",
                "Consenso por desgaste natural",
                List.of(vote1, vote2)
        );

        assertThat(eval.consensusVerdict()).isEqualTo(DamageVerdict.DESGASTE_NATURAL);
        assertThat(eval.isUnanimous()).isTrue();
        assertThat(eval.canIdentifyPartsClearly()).isTrue();
        assertThat(eval.requiresPresentialInspection()).isFalse();
        assertThat(eval.averageConfidence()).isEqualTo(0.875);
        assertThat(eval.votes()).hasSize(2);
    }

    @Test
    @DisplayName("Deve lançar exceção se lista de votos for nula ou vazia")
    void shouldThrowWhenVotesNullOrEmpty() {
        assertThatThrownBy(() -> new CommitteeEvaluation(
                DamageVerdict.DESGASTE_NATURAL,
                "0/0",
                false,
                true,
                false,
                0.0,
                "",
                "",
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Lista de votos");

        assertThatThrownBy(() -> new CommitteeEvaluation(
                DamageVerdict.DESGASTE_NATURAL,
                "0/0",
                false,
                true,
                false,
                0.0,
                "",
                "",
                Collections.emptyList()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Lista de votos");
    }
}

