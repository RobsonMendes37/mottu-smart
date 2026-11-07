package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;

import java.util.UUID;

public record MotorcycleFilter(
        MotorcycleStatus status,
        String plate,
        UUID yardId,
        MotorcycleModel model
) {
}
