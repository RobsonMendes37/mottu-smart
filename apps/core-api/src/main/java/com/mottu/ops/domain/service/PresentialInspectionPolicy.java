package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.domain.model.PresentialConvocatoria;

public class PresentialInspectionPolicy {

    public static final String DEFAULT_PRESENTIAL_ORIGIN = "DIAGNÓSTICO PRESENCIAL CONVOCADO";

    public PresentialConvocatoria buildPresentialConvocatoria(String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("O motivo da convocação presencial não pode ser vazio");
        }
        return new PresentialConvocatoria(
                true,
                Money.ZERO,
                DamageVerdict.DESGASTE_NATURAL,
                DEFAULT_PRESENTIAL_ORIGIN,
                reason.trim()
        );
    }
}
