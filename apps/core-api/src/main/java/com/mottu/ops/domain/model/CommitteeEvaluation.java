package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;

import java.util.List;

public record CommitteeEvaluation(
        DamageVerdict consensusVerdict,
        String consensusScore,
        boolean isUnanimous,
        boolean canIdentifyPartsClearly,
        boolean requiresPresentialInspection,
        double averageConfidence,
        String dissentingOpinion,
        String summary,
        List<SpecialistVote> votes
) {
    public CommitteeEvaluation {
        if (votes == null || votes.isEmpty()) {
            throw new IllegalArgumentException("Lista de votos não pode ser nula ou vazia");
        }
    }
}

