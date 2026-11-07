package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMotorcycleStatusCommand(
        @NotNull(message = "status is required")
        MotorcycleStatus status
) {
}
