package com.mottu.ops.domain.service;

import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.FraudDetectionResult;
import com.mottu.ops.domain.model.PhotoHash;
import com.mottu.ops.domain.model.PhotoMetadata;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PhotoFraudDetector {

    public static final String PHOTO_FRAUD_DETECTED = FraudDetectionResult.PHOTO_FRAUD_DETECTED;
    public static final int EXACT_MATCH_THRESHOLD = 0;
    public static final int NEAR_DUPLICATE_THRESHOLD = 3;

    public FraudDetectionResult evaluate(
            PhotoHash candidatePhoto,
            PhotoMetadata metadata,
            Collection<PhotoHash> existingPhotos
    ) {
        Objects.requireNonNull(candidatePhoto, "Candidate photo hash cannot be null");

        // 1. Metadata Telemetry Verification
        if (metadata != null && !metadata.hasValidCaptureTelemetry()) {
            return FraudDetectionResult.fraud(
                    PHOTO_FRAUD_DETECTED + ": Missing or tampered camera capture telemetry"
            );
        }

        if (existingPhotos == null || existingPhotos.isEmpty()) {
            return FraudDetectionResult.clean();
        }

        // 2. Historical Hash Collisions (SHA-256 and dHash)
        for (PhotoHash existing : existingPhotos) {
            if (candidatePhoto.isIdenticalSha256(existing)) {
                return FraudDetectionResult.fraud(
                        PHOTO_FRAUD_DETECTED + ": Identical SHA-256 hash detected with existing inspection photo"
                );
            }
            if (candidatePhoto.perceptualHash() != null && existing.perceptualHash() != null) {
                int distance = candidatePhoto.hammingDistance(existing);
                if (distance == EXACT_MATCH_THRESHOLD) {
                    return FraudDetectionResult.fraud(
                            PHOTO_FRAUD_DETECTED + ": Identical visual difference hash (dHash) detected"
                    );
                }
                if (distance <= NEAR_DUPLICATE_THRESHOLD) {
                    return FraudDetectionResult.fraud(
                            PHOTO_FRAUD_DETECTED + ": Near-identical visual difference hash detected (Hamming distance: " + distance + ")"
                    );
                }
            }
        }

        return FraudDetectionResult.clean();
    }

    public FraudDetectionResult checkDuplicatePhotosInBatch(List<PhotoHash> photos) {
        if (photos == null || photos.size() <= 1) {
            return FraudDetectionResult.clean();
        }

        Set<String> seenSha256 = new HashSet<>();
        for (PhotoHash p : photos) {
            if (p == null) continue;
            if (!seenSha256.add(p.sha256())) {
                return FraudDetectionResult.fraud(
                        PHOTO_FRAUD_DETECTED + ": Duplicate SHA-256 photo submitted in multiple slots of same inspection"
                );
            }
        }

        for (int i = 0; i < photos.size(); i++) {
            PhotoHash p1 = photos.get(i);
            if (p1 == null || p1.perceptualHash() == null) continue;
            for (int j = i + 1; j < photos.size(); j++) {
                PhotoHash p2 = photos.get(j);
                if (p2 != null && p2.perceptualHash() != null) {
                    int distance = p1.hammingDistance(p2);
                    if (distance <= NEAR_DUPLICATE_THRESHOLD) {
                        return FraudDetectionResult.fraud(
                                PHOTO_FRAUD_DETECTED + ": Duplicate visual photo submitted in multiple slots of same inspection (Hamming distance: " + distance + ")"
                        );
                    }
                }
            }
        }
        return FraudDetectionResult.clean();
    }

    public static String computeSha256(byte[] data) {
        return computeHash(data, "SHA-256");
    }

    public static String computeHash(byte[] data, String algorithm) {
        Objects.requireNonNull(data, "Image data cannot be null");
        if (data.length == 0) {
            throw new DomainValidationException("Image data cannot be empty");
        }
        try {
            byte[] hash = MessageDigest.getInstance(algorithm).digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new DomainValidationException("Invalid digest algorithm: " + algorithm);
        }
    }
}
