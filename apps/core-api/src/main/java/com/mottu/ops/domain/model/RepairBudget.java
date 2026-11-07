package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;

import java.util.Objects;

public record RepairBudget(
        DamageVerdict verdict,
        Money partsTotal,
        Money laborFee,
        Money totalEstimated,
        Money mottuAbsorbedCost
) {
    public RepairBudget {
        Objects.requireNonNull(verdict, "verdict cannot be null");
        Objects.requireNonNull(partsTotal, "partsTotal cannot be null");
        Objects.requireNonNull(laborFee, "laborFee cannot be null");
        Objects.requireNonNull(totalEstimated, "totalEstimated cannot be null");
        Objects.requireNonNull(mottuAbsorbedCost, "mottuAbsorbedCost cannot be null");
    }

    public static RepairBudget zero(DamageVerdict verdict) {
        return new RepairBudget(verdict, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
    }
}
