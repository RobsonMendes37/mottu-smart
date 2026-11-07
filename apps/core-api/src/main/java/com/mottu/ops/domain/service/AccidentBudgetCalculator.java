package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.VehicleDrivability;
import com.mottu.ops.domain.model.AccidentBudget;
import com.mottu.ops.domain.model.AccidentReportItem;
import com.mottu.ops.domain.model.Money;

import java.util.List;

public class AccidentBudgetCalculator {

    public static final Money DEFAULT_LIGHT_LABOR_FEE = Money.of(35.00);
    public static final Money DEFAULT_HEAVY_LABOR_FEE = Money.of(90.00);
    public static final Money DEFAULT_TOWING_FEE = Money.of(80.00);

    public AccidentBudget calculate(
            List<AccidentReportItem> items,
            VehicleDrivability drivability,
            Money lightLaborFee,
            Money heavyLaborFee,
            Money towingFee
    ) {
        if (items == null) {
            throw new IllegalArgumentException("Items cannot be null");
        }
        if (drivability == null) {
            throw new IllegalArgumentException("Drivability cannot be null");
        }

        Money effectiveLight = lightLaborFee != null ? lightLaborFee : DEFAULT_LIGHT_LABOR_FEE;
        Money effectiveHeavy = heavyLaborFee != null ? heavyLaborFee : DEFAULT_HEAVY_LABOR_FEE;
        Money effectiveTowing = towingFee != null ? towingFee : DEFAULT_TOWING_FEE;

        int totalPartsCount = items.stream().mapToInt(AccidentReportItem::getQuantity).sum();

        Money partsSubtotal = Money.ZERO;
        for (AccidentReportItem item : items) {
            partsSubtotal = partsSubtotal.add(item.getUnitPrice().multiply(item.getQuantity()));
        }

        Money laborFee;
        if (totalPartsCount == 0) {
            laborFee = Money.ZERO;
        } else if (totalPartsCount <= 2) {
            laborFee = effectiveLight;
        } else {
            laborFee = effectiveHeavy;
        }

        Money appliedTowingFee = (drivability == VehicleDrivability.NEEDS_TOWING)
                ? effectiveTowing
                : Money.ZERO;

        Money total = partsSubtotal.add(laborFee).add(appliedTowingFee);

        return new AccidentBudget(partsSubtotal, laborFee, appliedTowingFee, total, totalPartsCount);
    }
}
