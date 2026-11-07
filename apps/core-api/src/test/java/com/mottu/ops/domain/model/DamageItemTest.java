package com.mottu.ops.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageItemTest {

    @Test
    @DisplayName("Should create DamageItem and validate factory methods")
    void shouldCreateDamageItem() {
        DamageItem item1 = DamageItem.of("Manete", 25.0);
        assertEquals("Manete", item1.partName());
        assertEquals(Money.of(25.0), item1.unitPrice());
        assertFalse(item1.naturalWear());

        DamageItem item2 = DamageItem.of("Pastilha", 35.0, true);
        assertEquals("Pastilha", item2.partName());
        assertEquals(Money.of(35.0), item2.unitPrice());
        assertTrue(item2.naturalWear());
    }

    @Test
    @DisplayName("Null validations in DamageItem")
    void shouldValidateNullParameters() {
        assertThrows(NullPointerException.class, () -> new DamageItem(null, Money.of(10), false));
        assertThrows(NullPointerException.class, () -> new DamageItem("Part", null, false));
    }
}
