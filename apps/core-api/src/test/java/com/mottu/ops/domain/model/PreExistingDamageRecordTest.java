package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreExistingDamageRecordTest {

    @Test
    @DisplayName("Should create PreExistingDamageRecord and match part name")
    void shouldCreateAndMatchPart() {
        Instant now = Instant.now();
        PreExistingDamageRecord record = new PreExistingDamageRecord("Retrovisor Esquerdo", "Riscado", now, "sha");

        assertEquals("Retrovisor Esquerdo", record.partName());
        assertEquals("Riscado", record.description());
        assertEquals(now, record.recordedAt());
        assertEquals("sha", record.photoSha256());

        assertTrue(record.matchesPart("Retrovisor Esquerdo"));
        assertTrue(record.matchesPart("  retrovisor esquerdo  "));
        assertFalse(record.matchesPart("Manete"));
        assertFalse(record.matchesPart(null));
    }

    @Test
    @DisplayName("Constructor parameter validations")
    void shouldValidateConstructor() {
        Instant now = Instant.now();

        assertThrows(DomainValidationException.class, () ->
                new PreExistingDamageRecord(null, "desc", now, "sha"));
        assertThrows(DomainValidationException.class, () ->
                new PreExistingDamageRecord("   ", "desc", now, "sha"));
        assertThrows(DomainValidationException.class, () ->
                new PreExistingDamageRecord("Part", "desc", null, "sha"));
    }
}
