package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;

import java.util.UUID;

public record RentedMotorcycleDto(
        UUID id,
        String plate,
        MotorcycleModel model,
        MotorcycleStatus status,
        int currentOdometerKm,
        String yardName
) {
}
