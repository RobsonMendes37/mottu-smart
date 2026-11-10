package com.mottu.ops.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RecordBaseline360Command(
        @NotBlank(message = "Foto frontal é obrigatória")
        String frontPhotoUrl,

        @NotBlank(message = "Foto traseira é obrigatória")
        String rearPhotoUrl,

        @NotBlank(message = "Foto lateral direita é obrigatória")
        String rightSidePhotoUrl,

        @NotBlank(message = "Foto lateral esquerda é obrigatória")
        String leftSidePhotoUrl,

        @NotBlank(message = "Foto do odômetro é obrigatória")
        String odometerPhotoUrl,

        @NotNull(message = "Quilometragem inicial é obrigatória")
        @PositiveOrZero(message = "Quilometragem inicial não pode ser negativa")
        Integer initialOdometerKm
) {
}
