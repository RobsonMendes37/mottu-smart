package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoMetadataTest {

    @Test
    @DisplayName("Should create PhotoMetadata and validate getters")
    void shouldCreatePhotoMetadata() {
        Instant now = Instant.now();
        PhotoMetadata metadata = new PhotoMetadata(1920, 1080, 500_000, "image/jpeg", now, true);

        assertEquals(1920, metadata.width());
        assertEquals(1080, metadata.height());
        assertEquals(500_000, metadata.fileSizeBytes());
        assertEquals("image/jpeg", metadata.contentType());
        assertEquals(now, metadata.captureTimestamp());
        assertTrue(metadata.hasValidCaptureTelemetry());
    }

    @Test
    @DisplayName("Validation for dimensions, size, and content types")
    void shouldValidatePhotoMetadata() {
        Instant now = Instant.now();

        // width and height <= 0
        assertThrows(DomainValidationException.class, () ->
                new PhotoMetadata(0, 1080, 500_000, "image/jpeg", now, true));
        assertThrows(DomainValidationException.class, () ->
                new PhotoMetadata(1920, 0, 500_000, "image/jpeg", now, true));

        // file size <= 0 or > 10MB
        assertThrows(DomainValidationException.class, () ->
                new PhotoMetadata(1920, 1080, 0, "image/jpeg", now, true));
        assertThrows(DomainValidationException.class, () ->
                new PhotoMetadata(1920, 1080, 11 * 1024 * 1024, "image/jpeg", now, true));

        // invalid content type
        assertThrows(DomainValidationException.class, () ->
                new PhotoMetadata(1920, 1080, 500_000, "application/pdf", now, true));
        assertThrows(DomainValidationException.class, () ->
                new PhotoMetadata(1920, 1080, 500_000, null, now, true));
    }
}
