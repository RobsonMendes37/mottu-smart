package com.mottu.ops.domain.model;

import java.util.Objects;

public record AccidentBudget(
        Money partsSubtotal,
        Money laborFee,
        Money towingFee,
        Money totalEstimated,
        int partsCount
) {
    public AccidentBudget {
        Objects.requireNonNull(partsSubtotal, "partsSubtotal cannot be null");
        Objects.requireNonNull(laborFee, "laborFee cannot be null");
        Objects.requireNonNull(towingFee, "towingFee cannot be null");
        Objects.requireNonNull(totalEstimated, "totalEstimated cannot be null");
    }

    public static AccidentBudget zero() {
        return new AccidentBudget(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, 0);
    }
}

