package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.model.CommitteeEvaluation;
import com.mottu.ops.domain.model.SpecialistVote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MechanicCommitteeConsensusTest {

    private final MechanicCommitteeConsensusEvaluator evaluator = new MechanicCommitteeConsensusEvaluator();

    @Test
    @DisplayName("Deve formar consenso unânime quando os 3 peritos concordam com DESGASTE_NATURAL")
    void shouldFormUnanimousConsensusWhenAllAgreeOnNaturalWear() {
        List<SpecialistVote> votes = List.of(
                new SpecialistVote("PERITO_FADIGA_DESGASTE", DamageVerdict.DESGASTE_NATURAL, 0.95, "Alta rodagem (32.000km) compatível com desgaste de pastilha"),
                new SpecialistVote("PERITO_FORENSE_IMPACTO", DamageVerdict.DESGASTE_NATURAL, 0.90, "Sem marcas de atrito lateral, amassado ou impacto de asfalto"),
                new SpecialistVote("PERITO_COMPARADOR_360", DamageVerdict.DESGASTE_NATURAL, 0.92, "Evolução progressiva e uniforme em relação à vistoria 360 inicial")
        );

        CommitteeEvaluation evaluation = evaluator.evaluate(votes, true);

        assertThat(evaluation.isUnanimous()).isTrue();
        assertThat(evaluation.consensusVerdict()).isEqualTo(DamageVerdict.DESGASTE_NATURAL);
        assertThat(evaluation.consensusScore()).isEqualTo("3/3 Unânime");
        assertThat(evaluation.canIdentifyPartsClearly()).isTrue();
        assertThat(evaluation.requiresPresentialInspection()).isFalse();
        assertThat(evaluation.averageConfidence()).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    @DisplayName("Deve sinalizar divergência técnica (2x1) quando perito de impacto diverge de desgaste")
    void shouldSignalSplitVoteWhenForensicSpecialistDisagrees() {
        List<SpecialistVote> votes = List.of(
                new SpecialistVote("PERITO_FADIGA_DESGASTE", DamageVerdict.DESGASTE_NATURAL, 0.85, "Desgaste superficial da borracha"),
                new SpecialistVote("PERITO_FORENSE_IMPACTO", DamageVerdict.MAU_USO, 0.92, "Sulco profundo característico de atrito contínuo em guia/meio-fio"),
                new SpecialistVote("PERITO_COMPARADOR_360", DamageVerdict.MAU_USO, 0.88, "Dano não existia no registro 360 de entrega da moto")
        );

        CommitteeEvaluation evaluation = evaluator.evaluate(votes, true);

        assertThat(evaluation.isUnanimous()).isFalse();
        assertThat(evaluation.consensusVerdict()).isEqualTo(DamageVerdict.MAU_USO);
        assertThat(evaluation.consensusScore()).isEqualTo("2/3 Divergência Técnica (1 divergente)");
        assertThat(evaluation.dissentingOpinion()).contains("PERITO_FADIGA_DESGASTE");
    }

    @Test
    @DisplayName("Deve recomendar convocação presencial quando peritos sinalizam que a foto é insuficiente ou inconclusiva")
    void shouldRecommendPresentialWhenPhotosInconclusiveOrInternalDefect() {
        List<SpecialistVote> votes = List.of(
                new SpecialistVote("PERITO_FADIGA_DESGASTE", DamageVerdict.DESGASTE_NATURAL, 0.40, "Possível ruído ou embreagem patinando, foto não permite audição"),
                new SpecialistVote("PERITO_FORENSE_IMPACTO", DamageVerdict.PRE_EXISTENTE, 0.45, "Ângulo da foto com reflexo, componente interno inacessível"),
                new SpecialistVote("PERITO_COMPARADOR_360", DamageVerdict.DESGASTE_NATURAL, 0.50, "Inconclusivo comparado à entrega")
        );

        CommitteeEvaluation evaluation = evaluator.evaluate(votes, false);

        assertThat(evaluation.canIdentifyPartsClearly()).isFalse();
        assertThat(evaluation.requiresPresentialInspection()).isTrue();
        assertThat(evaluation.summary()).contains("Convocação para avaliação física");
    }

    @Test
    @DisplayName("Deve rejeitar lista de votos vazia ou menor que 2 especialistas")
    void shouldRejectInsufficientSpecialists() {
        assertThatThrownBy(() -> evaluator.evaluate(List.of(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comitê");
    }
}
