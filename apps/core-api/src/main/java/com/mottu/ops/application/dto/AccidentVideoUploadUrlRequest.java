package com.mottu.ops.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccidentVideoUploadUrlRequest(
        @NotBlank(message = "contentType é obrigatório")
        String contentType,

        @NotNull(message = "fileSizeBytes é obrigatório")
        @Min(value = 1, message = "fileSizeBytes deve ser maior que zero")
        @Max(value = 31457280, message = "fileSizeBytes excede o limite de 30 MB")
        Long fileSizeBytes,

        @NotNull(message = "durationSeconds é obrigatório")
        @Min(value = 1, message = "durationSeconds deve ser pelo menos 1 segundo")
        @Max(value = 30, message = "durationSeconds excede o limite de 30 segundos")
        Integer durationSeconds
) {
}

