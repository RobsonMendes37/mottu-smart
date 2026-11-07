package com.mottu.ops.infrastructure.adapter.out.storage;

import com.mottu.ops.application.dto.UploadUrlResponse;
import com.mottu.ops.application.port.out.StoragePort;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Component
public class MinioStorageAdapter implements StoragePort {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_MIME_TYPES = Set.of("video/mp4", "video/webm");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final long MAX_VIDEO_SIZE_BYTES = 30L * 1024 * 1024; // 30 MB
    private static final int MAX_VIDEO_DURATION_SECONDS = 30;
    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15); // 900s
    private static final Duration PRESIGNED_DOWNLOAD_EXPIRY = Duration.ofMinutes(60);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public MinioStorageAdapter(S3Presigner s3Presigner, S3Client s3Client, S3StorageProperties properties) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public UploadUrlResponse generatePresignedUploadUrl(String contentType, long fileSizeBytes, UUID inspectionId, String slot) {
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new DomainValidationException("Tipo de arquivo inválido. Permitido apenas image/jpeg, image/png ou image/webp");
        }
        if (fileSizeBytes <= 0) {
            throw new DomainValidationException("fileSizeBytes deve ser maior que zero");
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new DomainValidationException("fileSizeBytes excede o limite máximo de 10 MB");
        }

        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        String objectKey;
        if (inspectionId != null && slot != null && !slot.isBlank()) {
            String shortHash = UUID.randomUUID().toString().substring(0, 8);
            objectKey = "inspections/" + inspectionId + "/" + slot.trim() + "_" + shortHash + "." + extension;
        } else {
            LocalDate now = LocalDate.now();
            objectKey = String.format("inspections/%04d/%02d/%s.%s", now.getYear(), now.getMonthValue(), UUID.randomUUID(), extension);
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(fileSizeBytes)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRY)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        return new UploadUrlResponse(uploadUrl, objectKey, (int) PRESIGNED_URL_EXPIRY.toSeconds());
    }

    @Override
    public UploadUrlResponse generatePresignedVideoUploadUrl(String contentType, long fileSizeBytes, int durationSeconds) {
        if (contentType == null || !ALLOWED_VIDEO_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new DomainValidationException("Tipo de vídeo inválido. Permitido apenas video/mp4 ou video/webm");
        }
        if (fileSizeBytes <= 0) {
            throw new DomainValidationException("fileSizeBytes deve ser maior que zero");
        }
        if (fileSizeBytes > MAX_VIDEO_SIZE_BYTES) {
            throw new DomainValidationException("fileSizeBytes excede o limite máximo de 30 MB");
        }
        if (durationSeconds <= 0 || durationSeconds > MAX_VIDEO_DURATION_SECONDS) {
            throw new DomainValidationException("durationSeconds deve ser entre 1 e 30 segundos");
        }

        String extension = "video/webm".equalsIgnoreCase(contentType) ? "webm" : "mp4";
        LocalDate now = LocalDate.now();
        String objectKey = String.format("accidents/%04d/%02d/%s.%s", now.getYear(), now.getMonthValue(), UUID.randomUUID(), extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(fileSizeBytes)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRY)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        return new UploadUrlResponse(uploadUrl, objectKey, (int) PRESIGNED_URL_EXPIRY.toSeconds());
    }

    @Override
    public UploadUrlResponse generatePresignedAccidentPhotoUploadUrl(String contentType, long fileSizeBytes) {
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new DomainValidationException("Tipo de arquivo inválido. Permitido apenas image/jpeg, image/png ou image/webp");
        }
        if (fileSizeBytes <= 0) {
            throw new DomainValidationException("fileSizeBytes deve ser maior que zero");
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new DomainValidationException("fileSizeBytes excede o limite máximo de 10 MB");
        }

        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        LocalDate now = LocalDate.now();
        String objectKey = String.format("accidents/%04d/%02d/photo_%s.%s", now.getYear(), now.getMonthValue(), UUID.randomUUID(), extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(fileSizeBytes)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 300s
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        return new UploadUrlResponse(uploadUrl, objectKey, 300);
    }

    @Override
    public String generatePresignedDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .build();

        GetObjectPresignRequest getPresign = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_DOWNLOAD_EXPIRY)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(getPresign);
        return presigned.url().toString();
    }

    @Override
    public byte[] getObjectBytes(String photoObjectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(photoObjectKey)
                .build();
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
        return bytes.asByteArray();
    }

    @Override
    public boolean objectExists(String photoObjectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(photoObjectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
