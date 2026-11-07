package com.mottu.ops.domain.service;

import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.FraudDetectionResult;
import com.mottu.ops.domain.model.PhotoHash;
import com.mottu.ops.domain.model.PhotoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoFraudDetectorTest {

    private PhotoFraudDetector detector;
    private String cleanSha256;
    private String cleanDHash;
    private PhotoMetadata validMetadata;

    @BeforeEach
    void setUp() {
        detector = new PhotoFraudDetector();
        cleanSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        cleanDHash = "a1b2c3d4e5f60718";
        validMetadata = new PhotoMetadata(1920, 1080, 500_000, "image/jpeg", Instant.now(), true);
    }

    @Test
    @DisplayName("Clean photo with no historical match passes fraud check")
    void shouldPassCleanPhoto() {
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);
        List<PhotoHash> existing = List.of(
                new PhotoHash("f".repeat(64), "0000000000000000")
        );

        FraudDetectionResult result = detector.evaluate(candidate, validMetadata, existing);

        assertFalse(result.fraudSuspected());
        assertNull(result.reason());
    }

    @Test
    @DisplayName("Clean photo with empty or null existing photos passes fraud check")
    void shouldPassWhenExistingPhotosEmptyOrNull() {
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);

        FraudDetectionResult res1 = detector.evaluate(candidate, validMetadata, List.of());
        assertFalse(res1.fraudSuspected());

        FraudDetectionResult res2 = detector.evaluate(candidate, validMetadata, null);
        assertFalse(res2.fraudSuspected());
    }

    @Test
    @DisplayName("Identical SHA-256 collision flags fraud with PHOTO_FRAUD_DETECTED")
    void shouldDetectIdenticalSha256Collision() {
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);
        List<PhotoHash> existing = List.of(
                new PhotoHash(cleanSha256, "ffffffffffffffff")
        );

        FraudDetectionResult result = detector.evaluate(candidate, validMetadata, existing);

        assertTrue(result.fraudSuspected());
        assertTrue(result.reason().startsWith("PHOTO_FRAUD_DETECTED"));
        assertTrue(result.reason().contains("SHA-256"));
    }

    @Test
    @DisplayName("Identical visual difference hash (dHash distance 0) flags fraud")
    void shouldDetectIdenticalDHash() {
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);
        List<PhotoHash> existing = List.of(
                new PhotoHash("1".repeat(64), cleanDHash) // different SHA, same dHash
        );

        FraudDetectionResult result = detector.evaluate(candidate, validMetadata, existing);

        assertTrue(result.fraudSuspected());
        assertTrue(result.reason().startsWith("PHOTO_FRAUD_DETECTED"));
        assertTrue(result.reason().contains("dHash"));
    }

    @Test
    @DisplayName("Near-identical visual difference hash (Hamming distance <= 3) flags fraud")
    void shouldDetectNearIdenticalDHash() {
        // cleanDHash = "a1b2c3d4e5f60718"
        // '8' (1000) vs '9' (1001) -> 1 bit difference
        String nearDHash = "a1b2c3d4e5f60719";
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);
        List<PhotoHash> existing = List.of(
                new PhotoHash("2".repeat(64), nearDHash)
        );

        FraudDetectionResult result = detector.evaluate(candidate, validMetadata, existing);

        assertTrue(result.fraudSuspected());
        assertTrue(result.reason().contains("Near-identical"));
    }

    @Test
    @DisplayName("Different visual hash (Hamming distance > 3) passes")
    void shouldPassWhenHammingDistanceAboveThreshold() {
        // "0000000000000000" vs "ffffffffffffffff" -> 64 bits difference
        PhotoHash candidate = new PhotoHash(cleanSha256, "0000000000000000");
        List<PhotoHash> existing = List.of(
                new PhotoHash("3".repeat(64), "ffffffffffffffff")
        );

        FraudDetectionResult result = detector.evaluate(candidate, validMetadata, existing);

        assertFalse(result.fraudSuspected());
    }

    @Test
    @DisplayName("Missing or tampered telemetry flags fraud")
    void shouldDetectTamperedTelemetry() {
        PhotoMetadata tampered = new PhotoMetadata(1920, 1080, 500_000, "image/jpeg", Instant.now(), false);
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);

        FraudDetectionResult result = detector.evaluate(candidate, tampered, List.of());

        assertTrue(result.fraudSuspected());
        assertTrue(result.reason().contains("telemetry"));
    }

    @Test
    @DisplayName("Null metadata passes if telemetry check not applicable")
    void shouldPassWhenMetadataIsNull() {
        PhotoHash candidate = new PhotoHash(cleanSha256, cleanDHash);

        FraudDetectionResult result = detector.evaluate(candidate, null, List.of());

        assertFalse(result.fraudSuspected());
    }

    @Test
    @DisplayName("Duplicate photo submitted in multiple slots of same inspection batch flags fraud")
    void shouldDetectDuplicateInBatch() {
        PhotoHash p1 = new PhotoHash(cleanSha256, cleanDHash);
        PhotoHash p2 = new PhotoHash(cleanSha256, cleanDHash); // duplicate!

        FraudDetectionResult result = detector.checkDuplicatePhotosInBatch(List.of(p1, p2));

        assertTrue(result.fraudSuspected());
        assertTrue(result.reason().contains("multiple slots"));
    }

    @Test
    @DisplayName("Duplicate visual dHash submitted in multiple slots of same inspection batch flags fraud")
    void shouldDetectDuplicateVisualInBatch() {
        PhotoHash p1 = new PhotoHash(cleanSha256, cleanDHash);
        PhotoHash p2 = new PhotoHash("9".repeat(64), "a1b2c3d4e5f60719"); // 1 bit diff

        FraudDetectionResult result = detector.checkDuplicatePhotosInBatch(List.of(p1, p2));

        assertTrue(result.fraudSuspected());
        assertTrue(result.reason().contains("Duplicate visual photo"));
    }

    @Test
    @DisplayName("Batch check with unique photos returns clean")
    void shouldPassBatchWithUniquePhotos() {
        PhotoHash p1 = new PhotoHash("1".repeat(64), "0000000000000000");
        PhotoHash p2 = new PhotoHash("2".repeat(64), "ffffffffffffffff");
        PhotoHash p3 = new PhotoHash("3".repeat(64), null);

        FraudDetectionResult result = detector.checkDuplicatePhotosInBatch(List.of(p1, p2, p3));
        assertFalse(result.fraudSuspected());
    }

    @Test
    @DisplayName("Batch check with null or single photo returns clean")
    void shouldPassBatchWithZeroOrOnePhoto() {
        assertFalse(detector.checkDuplicatePhotosInBatch(null).fraudSuspected());
        assertFalse(detector.checkDuplicatePhotosInBatch(List.of()).fraudSuspected());
        assertFalse(detector.checkDuplicatePhotosInBatch(List.of(new PhotoHash(cleanSha256, cleanDHash))).fraudSuspected());
    }

    @Test
    @DisplayName("Batch check with null elements handled safely")
    void shouldHandleNullElementsInBatch() {
        List<PhotoHash> list = new ArrayList<>();
        list.add(null);
        list.add(new PhotoHash(cleanSha256, cleanDHash));
        list.add(null);

        assertFalse(detector.checkDuplicatePhotosInBatch(list).fraudSuspected());
    }

    @Test
    @DisplayName("Pure SHA-256 byte calculation produces correct 64-char hex")
    void shouldComputeSha256Correctly() {
        byte[] data = "mottu-smart-ops-test".getBytes(StandardCharsets.UTF_8);
        String hash = PhotoFraudDetector.computeSha256(data);

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[a-f0-9]{64}$"));
    }

    @Test
    @DisplayName("Invalid digest algorithm throws DomainValidationException")
    void shouldThrowOnInvalidAlgorithm() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(DomainValidationException.class, () ->
                PhotoFraudDetector.computeHash(data, "NON_EXISTENT_ALGO"));
    }

    @Test
    @DisplayName("Validation: Null candidate hash or empty bytes throw exceptions")
    void shouldThrowOnInvalidInput() {
        assertThrows(NullPointerException.class, () -> detector.evaluate(null, validMetadata, List.of()));
        assertThrows(NullPointerException.class, () -> PhotoFraudDetector.computeSha256(null));
        assertThrows(DomainValidationException.class, () -> PhotoFraudDetector.computeSha256(new byte[0]));
    }
}
