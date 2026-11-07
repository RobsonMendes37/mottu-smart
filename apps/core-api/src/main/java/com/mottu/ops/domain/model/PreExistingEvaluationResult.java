package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;

public record PreExistingEvaluationResult(
        boolean eligibleForExemption,
        DamageVerdict suggestedVerdict,
        String explanation
) {
    public static PreExistingEvaluationResult exempt(String explanation) {
        return new PreExistingEvaluationResult(true, DamageVerdict.PRE_EXISTENTE, explanation);
    }

    public static PreExistingEvaluationResult notExempt(String explanation) {
        return new PreExistingEvaluationResult(false, null, explanation);
    }
}
