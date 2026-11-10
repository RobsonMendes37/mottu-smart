package com.mottu.ops.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveContractDto(
        UUID contractId,
        Instant pickedUpAt,
        int initialOdometerKm,
        RentedMotorcycleDto motorcycle,
        String pickupPhotosJson,
        boolean pickupValidatedByRider,
        Instant pickupValidatedAt,
        boolean isShieldActive,
        double remainingShieldHours,
        int remainingShieldKm
) {
}
