package com.mottu.ops.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UploadUrlRequest(
        @NotBlank(message = "contentType is required")
        String contentType,

        @NotNull(message = "fileSizeBytes is required")
        @Min(value = 1, message = "fileSizeBytes must be greater than 0")
        @Max(value = 10485760, message = "fileSizeBytes cannot exceed 10 MB (10485760 bytes)")
        Long fileSizeBytes,

        UUID inspectionId,
        String slot
) {
}
