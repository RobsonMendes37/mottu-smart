package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreExistingEvaluationResultTest {

    @Test
    @DisplayName("Should create exempt and not-exempt results using static factories")
    void shouldCreateResults() {
        PreExistingEvaluationResult exempt = PreExistingEvaluationResult.exempt("Within 48h");
        assertTrue(exempt.eligibleForExemption());
        assertEquals(DamageVerdict.PRE_EXISTENTE, exempt.suggestedVerdict());
        assertEquals("Within 48h", exempt.explanation());

        PreExistingEvaluationResult notExempt = PreExistingEvaluationResult.notExempt("Window exceeded");
        assertFalse(notExempt.eligibleForExemption());
        assertNull(notExempt.suggestedVerdict());
        assertEquals("Window exceeded", notExempt.explanation());
    }
}
