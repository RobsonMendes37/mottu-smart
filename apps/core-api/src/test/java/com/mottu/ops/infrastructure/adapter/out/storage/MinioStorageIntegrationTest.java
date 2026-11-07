package com.mottu.ops.infrastructure.adapter.out.storage;

import com.mottu.ops.application.dto.UploadUrlResponse;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MinioStorageIntegrationTest {

    @Autowired
    private MinioStorageAdapter storageAdapter;

    @Autowired
    private S3StorageProperties storageProperties;

    @Test
    @DisplayName("generatePresignedUploadUrl computes valid AWS SigV4 PUT URL matching S3_PUBLIC_ENDPOINT")
    void shouldGeneratePresignedPutUrl() {
        UUID inspectionId = UUID.randomUUID();
        String slot = "odometer";

        UploadUrlResponse response = storageAdapter.generatePresignedUploadUrl(
                "image/jpeg",
                450_000L,
                inspectionId,
                slot
        );

        assertNotNull(response);
        assertNotNull(response.uploadUrl());
        assertTrue(response.uploadUrl().startsWith(storageProperties.getPublicEndpoint()),
                "Upload URL must use public endpoint: " + storageProperties.getPublicEndpoint());
        assertTrue(response.uploadUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"),
                "Upload URL must contain SigV4 query parameters");
        assertTrue(response.uploadUrl().contains("X-Amz-Signature="));
        assertEquals(900, response.expiresInSeconds());
        assertTrue(response.photoObjectKey().contains(inspectionId.toString()));
        assertTrue(response.photoObjectKey().endsWith(".jpg"));
    }

    @Test
    @DisplayName("generatePresignedUploadUrl with default date-based key when inspectionId is omitted")
    void shouldGeneratePresignedPutUrlWithDateKey() {
        UploadUrlResponse response = storageAdapter.generatePresignedUploadUrl(
                "image/webp",
                300_000L,
                null,
                null
        );

        assertNotNull(response);
        assertNotNull(response.uploadUrl());
        assertTrue(response.photoObjectKey().startsWith("inspections/"));
        assertTrue(response.photoObjectKey().endsWith(".webp"));
    }

    @Test
    @DisplayName("generatePresignedUploadUrl rejects unsupported MIME types")
    void shouldRejectUnsupportedMimeType() {
        assertThrows(DomainValidationException.class, () ->
                storageAdapter.generatePresignedUploadUrl("text/plain", 1024L, null, null));
        assertThrows(DomainValidationException.class, () ->
                storageAdapter.generatePresignedUploadUrl("image/gif", 1024L, null, null));
        assertThrows(DomainValidationException.class, () ->
                storageAdapter.generatePresignedUploadUrl("application/pdf", 1024L, null, null));
    }

    @Test
    @DisplayName("generatePresignedUploadUrl rejects zero or negative or exceeding file size")
    void shouldRejectInvalidFileSize() {
        assertThrows(DomainValidationException.class, () ->
                storageAdapter.generatePresignedUploadUrl("image/png", 0L, null, null));
        assertThrows(DomainValidationException.class, () ->
                storageAdapter.generatePresignedUploadUrl("image/png", -10L, null, null));
        assertThrows(DomainValidationException.class, () ->
                storageAdapter.generatePresignedUploadUrl("image/png", 11L * 1024 * 1024, null, null));
    }
}
