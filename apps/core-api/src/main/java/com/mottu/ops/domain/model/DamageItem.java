package com.mottu.ops.domain.model;

import java.util.Objects;

public record DamageItem(
        String partName,
        Money unitPrice,
        boolean naturalWear
) {
    public DamageItem {
        Objects.requireNonNull(partName, "partName cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
    }

    public static DamageItem of(String partName, double unitPrice, boolean naturalWear) {
        return new DamageItem(partName, Money.of(unitPrice), naturalWear);
    }

    public static DamageItem of(String partName, double unitPrice) {
        return new DamageItem(partName, Money.of(unitPrice), false);
    }
}
