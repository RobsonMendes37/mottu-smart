package com.mottu.ops.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FraudDetectionResultTest {

    @Test
    @DisplayName("Should create clean and fraud results using static factories")
    void shouldCreateResults() {
        FraudDetectionResult clean = FraudDetectionResult.clean();
        assertFalse(clean.fraudSuspected());
        assertNull(clean.reason());

        FraudDetectionResult fraud = FraudDetectionResult.fraud("PHOTO_FRAUD_DETECTED: SHA-256 match");
        assertTrue(fraud.fraudSuspected());
        assertEquals("PHOTO_FRAUD_DETECTED: SHA-256 match", fraud.reason());
    }
}
