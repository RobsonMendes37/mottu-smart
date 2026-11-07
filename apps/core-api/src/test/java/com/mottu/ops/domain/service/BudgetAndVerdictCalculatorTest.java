package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.DamageItem;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.domain.model.RepairBudget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetAndVerdictCalculatorTest {

    private BudgetAndVerdictCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BudgetAndVerdictCalculator();
    }

    @Test
    @DisplayName("TC-BDG-01: Natural wear part under DESGASTE_NATURAL results in R$ 0.00 rider cost and absorbs parts + labor")
    void shouldCalculateZeroForNaturalWear() {
        List<DamageItem> items = List.of(
                DamageItem.of("Sapata de Freio", 33.00, true)
        );

        RepairBudget budget = calculator.calculateBudget(DamageVerdict.DESGASTE_NATURAL, items);

        assertEquals(DamageVerdict.DESGASTE_NATURAL, budget.verdict());
        assertEquals(Money.ZERO, budget.partsTotal());
        assertEquals(Money.ZERO, budget.laborFee());
        assertEquals(Money.ZERO, budget.totalEstimated());
        // Mottu absorbs: R$ 33.00 + R$ 35.00 = R$ 68.00
        assertEquals(Money.of(68.00), budget.mottuAbsorbedCost());
    }

    @Test
    @DisplayName("TC-BDG-02: Single part under MAU_USO charges part plus single standard labor fee R$ 35.00")
    void shouldCalculateSinglePartMisuse() {
        List<DamageItem> items = List.of(
                DamageItem.of("Manete de Freio", 25.00, false)
        );

        RepairBudget budget = calculator.calculateBudget(DamageVerdict.MAU_USO, items);

        assertEquals(DamageVerdict.MAU_USO, budget.verdict());
        assertEquals(Money.of(25.00), budget.partsTotal());
        assertEquals(Money.of(35.00), budget.laborFee());
        assertEquals(Money.of(60.00), budget.totalEstimated());
        assertEquals(Money.ZERO, budget.mottuAbsorbedCost());
    }

    @Test
    @DisplayName("TC-BDG-03 (UC-06): Multiple parts under MAU_USO charge single labor fee only (R$ 25,00 + R$ 42,50 + R$ 35,00 = R$ 102,50)")
    void shouldChargeLaborFeeOnlyOnceForMultipleParts() {
        List<DamageItem> items = List.of(
                DamageItem.of("Manete de Freio", 25.00, false),
                DamageItem.of("Retrovisor", 42.50, false)
        );

        RepairBudget budget = calculator.calculateBudget(DamageVerdict.MAU_USO, items);

        assertEquals(DamageVerdict.MAU_USO, budget.verdict());
        assertEquals(Money.of(67.50), budget.partsTotal());
        assertEquals(Money.of(35.00), budget.laborFee());
        assertEquals(Money.of(102.50), budget.totalEstimated());
        assertEquals(Money.ZERO, budget.mottuAbsorbedCost());
    }

    @Test
    @DisplayName("TC-BDG-04: Pre-existing damage under PRE_EXISTENTE results in R$ 0.00 rider cost and absorbs parts + labor")
    void shouldCalculateZeroForPreExisting() {
        List<DamageItem> items = List.of(
                DamageItem.of("Carenagem Traseira", 150.00, false)
        );

        RepairBudget budget = calculator.calculateBudget(DamageVerdict.PRE_EXISTENTE, items);

        assertEquals(DamageVerdict.PRE_EXISTENTE, budget.verdict());
        assertEquals(Money.ZERO, budget.partsTotal());
        assertEquals(Money.ZERO, budget.laborFee());
        assertEquals(Money.ZERO, budget.totalEstimated());
        assertEquals(Money.of(185.00), budget.mottuAbsorbedCost());
    }

    @Test
    @DisplayName("TC-BDG-05: Custom labor fee (e.g. R$ 50.00 from Manager Settings) is applied correctly")
    void shouldApplyCustomLaborFee() {
        List<DamageItem> items = List.of(
                DamageItem.of("Manete de Freio", 25.00, false)
        );
        Money customLabor = Money.of(50.00);

        RepairBudget budget = calculator.calculateBudget(DamageVerdict.MAU_USO, items, customLabor);

        assertEquals(Money.of(25.00), budget.partsTotal());
        assertEquals(customLabor, budget.laborFee());
        assertEquals(Money.of(75.00), budget.totalEstimated());

        // When laborFee is null, falls back to DEFAULT_STANDARD_LABOR_FEE
        RepairBudget defaultBudget = calculator.calculateBudget(DamageVerdict.MAU_USO, items, null);
        assertEquals(BudgetAndVerdictCalculator.DEFAULT_STANDARD_LABOR_FEE, defaultBudget.laborFee());
        assertEquals(Money.of(60.00), defaultBudget.totalEstimated());
    }

    @Test
    @DisplayName("TC-BDG-06: Empty or null items list under MAU_USO returns zero budget")
    void shouldReturnZeroBudgetForEmptyItems() {
        RepairBudget b1 = calculator.calculateBudget(DamageVerdict.MAU_USO, Collections.emptyList());
        assertEquals(Money.ZERO, b1.partsTotal());
        assertEquals(Money.ZERO, b1.laborFee());
        assertEquals(Money.ZERO, b1.totalEstimated());

        RepairBudget b2 = calculator.calculateBudget(DamageVerdict.MAU_USO, null);
        assertEquals(Money.ZERO, b2.partsTotal());
        assertEquals(Money.ZERO, b2.laborFee());
        assertEquals(Money.ZERO, b2.totalEstimated());
    }

    @Test
    @DisplayName("TC-BDG-07: Negative part price throws DomainValidationException")
    void shouldThrowOnNegativePartPrice() {
        List<DamageItem> items = List.of(
                new DamageItem("Peça com preço negativo", Money.of(-10.00), false)
        );

        assertThrows(DomainValidationException.class, () ->
                calculator.calculateBudget(DamageVerdict.MAU_USO, items));
    }

    @Test
    @DisplayName("TC-BDG-08: Determine verdict for RECEIVED_LIKE_THIS and eligible pre-existing -> PRE_EXISTENTE")
    void shouldDeterminePreExistingVerdict() {
        DamageVerdict v1 = calculator.determineVerdict(ReportedOriginContext.RECEIVED_LIKE_THIS, true, false);
        assertEquals(DamageVerdict.PRE_EXISTENTE, v1);

        // Even if all parts are natural wear, if eligible for pre-existing handover exemption it takes priority
        DamageVerdict v2 = calculator.determineVerdict(ReportedOriginContext.RECEIVED_LIKE_THIS, true, true);
        assertEquals(DamageVerdict.PRE_EXISTENTE, v2);
    }

    @Test
    @DisplayName("TC-BDG-09: Determine verdict for NORMAL_USE_WEAR and all parts natural wear -> DESGASTE_NATURAL")
    void shouldDetermineNaturalWearVerdict() {
        DamageVerdict v1 = calculator.determineVerdict(ReportedOriginContext.NORMAL_USE_WEAR, false, true);
        assertEquals(DamageVerdict.DESGASTE_NATURAL, v1);

        // If not all parts are natural wear -> MAU_USO
        DamageVerdict v2 = calculator.determineVerdict(ReportedOriginContext.NORMAL_USE_WEAR, false, false);
        assertEquals(DamageVerdict.MAU_USO, v2);
    }

    @Test
    @DisplayName("TC-BDG-10: Determine verdict for ACCIDENT_OR_DROP or ineligible -> MAU_USO")
    void shouldDetermineMauUsoVerdict() {
        DamageVerdict v1 = calculator.determineVerdict(ReportedOriginContext.ACCIDENT_OR_DROP, false, false);
        assertEquals(DamageVerdict.MAU_USO, v1);

        DamageVerdict v2 = calculator.determineVerdict(ReportedOriginContext.RECEIVED_LIKE_THIS, false, false);
        assertEquals(DamageVerdict.MAU_USO, v2);
    }

    @Test
    @DisplayName("Validation: Null verdict throws NullPointerException")
    void shouldThrowOnNullVerdict() {
        assertThrows(NullPointerException.class, () ->
                calculator.calculateBudget(null, Collections.emptyList()));
        assertThrows(NullPointerException.class, () ->
                calculator.determineVerdict(null, true, true));
    }
}
