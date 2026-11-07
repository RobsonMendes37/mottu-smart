package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;

import java.time.Instant;

public record PhotoMetadata(
        int width,
        int height,
        long fileSizeBytes,
        String contentType,
        Instant captureTimestamp,
        boolean hasValidCaptureTelemetry
) {
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    public PhotoMetadata {
        if (width <= 0) throw new DomainValidationException("Image width must be positive");
        if (height <= 0) throw new DomainValidationException("Image height must be positive");
        if (fileSizeBytes <= 0 || fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new DomainValidationException("File size must be between 1 byte and 10 MB");
        }
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new DomainValidationException("Unsupported content type: " + contentType);
        }
    }
}
