package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;

public record PresentialConvocatoria(
        boolean presentialRequested,
        Money immediateCost,
        DamageVerdict recommendedVerdict,
        String originLabel,
        String reason
) {
    public PresentialConvocatoria {
        if (immediateCost == null) {
            throw new IllegalArgumentException("O custo imediato não pode ser nulo");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("O motivo da convocação presencial é obrigatório");
        }
    }
}

