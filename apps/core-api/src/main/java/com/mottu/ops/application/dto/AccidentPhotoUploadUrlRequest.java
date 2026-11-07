package com.mottu.ops.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccidentPhotoUploadUrlRequest(
        @NotBlank(message = "contentType é obrigatório")
        String contentType,

        @NotNull(message = "fileSizeBytes é obrigatório")
        @Min(value = 1, message = "fileSizeBytes deve ser maior que zero")
        @Max(value = 10485760, message = "fileSizeBytes excede o limite de 10 MB")
        Long fileSizeBytes
) {
}

