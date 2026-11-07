package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateMotorcycleCommand(
        @NotBlank(message = "plate is required")
        @Pattern(regexp = "^[A-Za-z0-9]{7,10}$", message = "plate must be between 7 and 10 alphanumeric characters")
        String plate,

        @NotNull(message = "model is required")
        MotorcycleModel model,

        @NotNull(message = "yardId is required")
        UUID yardId,

        @NotNull(message = "currentOdometerKm is required")
        @Min(value = 0, message = "currentOdometerKm cannot be negative")
        Integer currentOdometerKm
) {
}
