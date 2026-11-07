package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YardTest {

    @Test
    @DisplayName("Should create yard and handle activation/deactivation")
    void shouldCreateYardAndToggleStatus() {
        UUID id = UUID.randomUUID();
        Yard yard = new Yard(id, "Galpão Butantã", "São Paulo", "Av. Vital Brasil, 1000", true);

        assertEquals(id, yard.getId());
        assertEquals("Galpão Butantã", yard.getName());
        assertEquals("São Paulo", yard.getCity());
        assertEquals("Av. Vital Brasil, 1000", yard.getAddress());
        assertTrue(yard.isActive());

        yard.deactivate();
        assertFalse(yard.isActive());

        yard.activate();
        assertTrue(yard.isActive());

        Yard yardWithGeneratedId = new Yard(null, "Galpão Lapa", "SP", "Rua X", false);
        assertNotNull(yardWithGeneratedId.getId());
    }

    @Test
    @DisplayName("Constructor parameter validations")
    void shouldValidateYardConstructor() {
        assertThrows(DomainValidationException.class, () ->
                new Yard(null, null, "SP", "Rua X", true));
        assertThrows(DomainValidationException.class, () ->
                new Yard(null, "   ", "SP", "Rua X", true));
        assertThrows(DomainValidationException.class, () ->
                new Yard(null, "Galpão", null, "Rua X", true));
        assertThrows(DomainValidationException.class, () ->
                new Yard(null, "Galpão", "  ", "Rua X", true));
        assertThrows(DomainValidationException.class, () ->
                new Yard(null, "Galpão", "SP", null, true));
        assertThrows(DomainValidationException.class, () ->
                new Yard(null, "Galpão", "SP", "  ", true));
    }
}
