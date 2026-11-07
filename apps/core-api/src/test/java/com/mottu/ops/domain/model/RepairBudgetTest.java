package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepairBudgetTest {

    @Test
    @DisplayName("Should create RepairBudget and validate zero helper")
    void shouldCreateRepairBudget() {
        RepairBudget budget = new RepairBudget(
                DamageVerdict.MAU_USO,
                Money.of(50.0),
                Money.of(35.0),
                Money.of(85.0),
                Money.ZERO
        );

        assertEquals(DamageVerdict.MAU_USO, budget.verdict());
        assertEquals(Money.of(50.0), budget.partsTotal());
        assertEquals(Money.of(35.0), budget.laborFee());
        assertEquals(Money.of(85.0), budget.totalEstimated());
        assertEquals(Money.ZERO, budget.mottuAbsorbedCost());

        RepairBudget zero = RepairBudget.zero(DamageVerdict.MAU_USO);
        assertEquals(DamageVerdict.MAU_USO, zero.verdict());
        assertEquals(Money.ZERO, zero.partsTotal());
        assertEquals(Money.ZERO, zero.laborFee());
        assertEquals(Money.ZERO, zero.totalEstimated());
        assertEquals(Money.ZERO, zero.mottuAbsorbedCost());
    }

    @Test
    @DisplayName("Null validations in RepairBudget")
    void shouldValidateNullParameters() {
        assertThrows(NullPointerException.class, () ->
                new RepairBudget(null, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO));
        assertThrows(NullPointerException.class, () ->
                new RepairBudget(DamageVerdict.MAU_USO, null, Money.ZERO, Money.ZERO, Money.ZERO));
        assertThrows(NullPointerException.class, () ->
                new RepairBudget(DamageVerdict.MAU_USO, Money.ZERO, null, Money.ZERO, Money.ZERO));
        assertThrows(NullPointerException.class, () ->
                new RepairBudget(DamageVerdict.MAU_USO, Money.ZERO, Money.ZERO, null, Money.ZERO));
        assertThrows(NullPointerException.class, () ->
                new RepairBudget(DamageVerdict.MAU_USO, Money.ZERO, Money.ZERO, Money.ZERO, null));
    }
}
