package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InspectionItemTest {

    @Test
    @DisplayName("Should create InspectionItem with valid attributes")
    void shouldCreateInspectionItem() {
        UUID id = UUID.randomUUID();
        UUID inspectionId = UUID.randomUUID();
        UUID catalogPartId = UUID.randomUUID();
        Money unitPrice = Money.of(45.50);

        InspectionItem item = new InspectionItem(
                id,
                inspectionId,
                catalogPartId,
                "Espelho Retrovisor",
                unitPrice,
                "AI"
        );

        assertEquals(id, item.getId());
        assertEquals(inspectionId, item.getInspectionId());
        assertEquals(catalogPartId, item.getCatalogPartId());
        assertEquals("Espelho Retrovisor", item.getPartNameSnapshot());
        assertEquals(unitPrice, item.getUnitPriceSnapshot());
        assertEquals("AI", item.getAddedBy());

        InspectionItem itemWithGeneratedId = new InspectionItem(
                null,
                inspectionId,
                catalogPartId,
                "Espelho",
                unitPrice,
                "MECHANIC"
        );
        assertNotNull(itemWithGeneratedId.getId());
    }

    @Test
    @DisplayName("Constructor parameter validations")
    void shouldValidateConstructor() {
        UUID validId = UUID.randomUUID();

        // null inspectionId
        assertThrows(DomainValidationException.class, () ->
                new InspectionItem(validId, null, validId, "Name", Money.of(10), "AI"));

        // null or blank partNameSnapshot
        assertThrows(DomainValidationException.class, () ->
                new InspectionItem(validId, validId, validId, null, Money.of(10), "AI"));
        assertThrows(DomainValidationException.class, () ->
                new InspectionItem(validId, validId, validId, "   ", Money.of(10), "AI"));

        // null unitPriceSnapshot
        assertThrows(DomainValidationException.class, () ->
                new InspectionItem(validId, validId, validId, "Name", null, "AI"));

        // invalid addedBy (not AI or MECHANIC)
        assertThrows(DomainValidationException.class, () ->
                new InspectionItem(validId, validId, validId, "Name", Money.of(10), "OTHER"));
        assertThrows(DomainValidationException.class, () ->
                new InspectionItem(validId, validId, validId, "Name", Money.of(10), null));
    }
}
