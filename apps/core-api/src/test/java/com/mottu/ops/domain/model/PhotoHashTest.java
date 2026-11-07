package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoHashTest {

    @Test
    @DisplayName("Should create PhotoHash and handle comparisons and Hamming distance")
    void shouldCreatePhotoHashAndComputeDistance() {
        String sha1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String sha2 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String sha3 = "f3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        String dhash1 = "a1b2c3d4e5f60718";
        String dhash2 = "a1b2c3d4e5f60719"; // 1 bit diff: 8 (1000) vs 9 (1001)

        PhotoHash p1 = new PhotoHash(sha1, dhash1);
        PhotoHash p2 = new PhotoHash(sha2, dhash2);
        PhotoHash p3 = new PhotoHash(sha3, null);

        assertTrue(p1.isIdenticalSha256(p2));
        assertFalse(p1.isIdenticalSha256(p3));
        assertFalse(p1.isIdenticalSha256(null));

        assertFalse(p1.isIdenticalPerceptualHash(p2));
        assertFalse(p1.isIdenticalPerceptualHash(p3));
        assertFalse(p1.isIdenticalPerceptualHash(null));
        assertTrue(p1.isIdenticalPerceptualHash(new PhotoHash(sha1, dhash1)));

        assertEquals(1, p1.hammingDistance(p2));

        assertThrows(NullPointerException.class, () -> p1.hammingDistance(null));
        assertThrows(DomainValidationException.class, () -> p1.hammingDistance(p3));
        assertThrows(DomainValidationException.class, () -> p3.hammingDistance(p1));
    }

    @Test
    @DisplayName("Hamming distance static helper validation")
    void shouldValidateHammingDistanceCalculations() {
        assertEquals(0, PhotoHash.calculateHammingDistance("1234", "1234"));
        assertEquals(16, PhotoHash.calculateHammingDistance("0000", "ffff"));
        assertEquals(4, PhotoHash.calculateHammingDistance("0000", "000f"));

        assertThrows(DomainValidationException.class, () -> PhotoHash.calculateHammingDistance(null, "1234"));
        assertThrows(DomainValidationException.class, () -> PhotoHash.calculateHammingDistance("1234", null));
        assertThrows(DomainValidationException.class, () -> PhotoHash.calculateHammingDistance("123", "1234"));
        assertThrows(DomainValidationException.class, () -> PhotoHash.calculateHammingDistance("123g", "1234"));
    }

    @Test
    @DisplayName("PhotoHash constructor validation")
    void shouldValidateConstructor() {
        assertThrows(DomainValidationException.class, () -> new PhotoHash(null, "1234"));
        assertThrows(DomainValidationException.class, () -> new PhotoHash("short", "1234"));
    }
}
