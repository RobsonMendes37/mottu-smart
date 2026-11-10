package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.model.CommitteeEvaluation;
import com.mottu.ops.domain.model.SpecialistVote;

import java.util.*;
import java.util.stream.Collectors;

public class MechanicCommitteeConsensusEvaluator {

    public CommitteeEvaluation evaluate(List<SpecialistVote> votes, boolean partsClearlyVisible) {
        if (votes == null || votes.size() < 2) {
            throw new IllegalArgumentException("O comitê de avaliação requer no mínimo 2 peritos especializados");
        }

        double avgConfidence = votes.stream()
                .mapToDouble(SpecialistVote::confidence)
                .average()
                .orElse(0.0);

        Map<DamageVerdict, Long> voteCounts = votes.stream()
                .collect(Collectors.groupingBy(SpecialistVote::verdict, Collectors.counting()));

        DamageVerdict consensusVerdict = Collections.max(
                voteCounts.entrySet(),
                Map.Entry.<DamageVerdict, Long>comparingByValue()
                        .thenComparing(entry -> entry.getKey() == DamageVerdict.DESGASTE_NATURAL ? 1 : 0)
        ).getKey();

        long matchingVotes = voteCounts.get(consensusVerdict);
        int totalVotes = votes.size();
        boolean isUnanimous = (matchingVotes == totalVotes);

        String consensusScore;
        String dissentingOpinion = null;

        if (isUnanimous) {
            consensusScore = String.format("%d/%d Unânime", totalVotes, totalVotes);
        } else {
            long dissentingCount = totalVotes - matchingVotes;
            consensusScore = String.format("%d/%d Divergência Técnica (%d divergente%s)",
                    matchingVotes, totalVotes, dissentingCount, dissentingCount > 1 ? "s" : "");

            dissentingOpinion = votes.stream()
                    .filter(v -> v.verdict() != consensusVerdict)
                    .map(v -> String.format("%s votou %s: %s", v.specialistRole(), v.verdict(), v.reasoning()))
                    .collect(Collectors.joining("; "));
        }

        boolean requiresPresential = !partsClearlyVisible || avgConfidence < 0.60;
        String summary;

        if (requiresPresential) {
            summary = "Convocação para avaliação física presencial no galpão recomendada: fotos ou indícios não são 100% conclusivos por imagem remota.";
        } else if (isUnanimous) {
            summary = String.format("Consenso unânime dos peritos (%s) com confiança média de %.0f%%.",
                    consensusVerdict.name(), avgConfidence * 100);
        } else {
            summary = String.format("Decisão majoritária (%s) com divergência técnica registrada para revisão do mecânico na bancada.",
                    consensusScore);
        }

        return new CommitteeEvaluation(
                consensusVerdict,
                consensusScore,
                isUnanimous,
                partsClearlyVisible,
                requiresPresential,
                avgConfidence,
                dissentingOpinion,
                summary,
                votes
        );
    }
}
