package com.mottu.ops.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContestInspectionRequest(
        @NotBlank(message = "reason é obrigatório")
        @Size(min = 10, message = "reason deve ter no mínimo 10 caracteres")
        String reason
) {
}
