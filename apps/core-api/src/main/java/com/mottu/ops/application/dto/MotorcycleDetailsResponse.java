package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;

import java.time.Instant;
import java.util.UUID;

public record MotorcycleDetailsResponse(
        UUID id,
        String plate,
        MotorcycleModel model,
        UUID yardId,
        String yardName,
        MotorcycleStatus status,
        int currentOdometerKm,
        String activeRiderName,
        Instant createdAt,
        String pickupPhotosJson
) {
}
