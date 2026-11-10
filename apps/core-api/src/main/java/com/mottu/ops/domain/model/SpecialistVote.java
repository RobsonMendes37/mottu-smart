package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;

public record SpecialistVote(
        String specialistRole,
        DamageVerdict verdict,
        double confidence,
        String reasoning
) {
    public SpecialistVote {
        if (specialistRole == null || specialistRole.isBlank()) {
            throw new IllegalArgumentException("O papel do perito é obrigatório");
        }
        if (verdict == null) {
            throw new IllegalArgumentException("O veredito do perito é obrigatório");
        }
    }
}
