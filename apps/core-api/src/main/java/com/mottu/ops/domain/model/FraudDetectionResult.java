package com.mottu.ops.domain.model;

public record FraudDetectionResult(boolean fraudSuspected, String reason) {
    public static final String PHOTO_FRAUD_DETECTED = "PHOTO_FRAUD_DETECTED";

    public static FraudDetectionResult clean() {
        return new FraudDetectionResult(false, null);
    }

    public static FraudDetectionResult fraud(String reason) {
        return new FraudDetectionResult(true, reason);
    }
}
