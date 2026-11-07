package com.mottu.ops.application.port.out;

import com.mottu.ops.application.dto.UploadUrlResponse;

import java.util.UUID;

public interface StoragePort {
    UploadUrlResponse generatePresignedUploadUrl(String contentType, long fileSizeBytes, UUID inspectionId, String slot);
    UploadUrlResponse generatePresignedVideoUploadUrl(String contentType, long fileSizeBytes, int durationSeconds);
    UploadUrlResponse generatePresignedAccidentPhotoUploadUrl(String contentType, long fileSizeBytes);
    String generatePresignedDownloadUrl(String objectKey);
    byte[] getObjectBytes(String photoObjectKey);
    boolean objectExists(String photoObjectKey);
}
