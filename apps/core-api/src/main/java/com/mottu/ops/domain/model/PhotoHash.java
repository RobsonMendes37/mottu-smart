package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;

import java.util.Objects;

public record PhotoHash(String sha256, String perceptualHash) {
    public PhotoHash {
        if (sha256 == null || sha256.length() != 64) {
            throw new DomainValidationException("SHA-256 hash must be exactly 64 hexadecimal characters");
        }
        sha256 = sha256.toLowerCase();
        if (perceptualHash != null) {
            perceptualHash = perceptualHash.toLowerCase().trim();
        }
    }

    public boolean isIdenticalSha256(PhotoHash other) {
        if (other == null) return false;
        return this.sha256.equalsIgnoreCase(other.sha256);
    }

    public boolean isIdenticalPerceptualHash(PhotoHash other) {
        if (other == null || this.perceptualHash == null || other.perceptualHash == null) {
            return false;
        }
        return this.perceptualHash.equalsIgnoreCase(other.perceptualHash);
    }

    public int hammingDistance(PhotoHash other) {
        Objects.requireNonNull(other, "Other photo hash cannot be null");
        if (this.perceptualHash == null || other.perceptualHash == null) {
            throw new DomainValidationException("Cannot calculate Hamming distance when perceptual hash is null");
        }
        return calculateHammingDistance(this.perceptualHash, other.perceptualHash);
    }

    public static int calculateHammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            throw new DomainValidationException("Hashes for Hamming distance cannot be null");
        }
        if (hash1.length() != hash2.length()) {
            throw new DomainValidationException("Hashes must have identical length for Hamming distance calculation");
        }
        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            int val1 = Character.digit(hash1.charAt(i), 16);
            int val2 = Character.digit(hash2.charAt(i), 16);
            if (val1 < 0 || val2 < 0) {
                throw new DomainValidationException("Invalid hexadecimal character in hash");
            }
            distance += Integer.bitCount(val1 ^ val2);
        }
        return distance;
    }
}
