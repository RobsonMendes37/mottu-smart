package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.UploadUrlRequest;
import com.mottu.ops.application.dto.UploadUrlResponse;
import com.mottu.ops.application.port.in.GenerateUploadUrlUseCase;
import com.mottu.ops.application.port.out.StoragePort;

public class UploadUrlService implements GenerateUploadUrlUseCase {

    private final StoragePort storagePort;

    public UploadUrlService(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @Override
    public UploadUrlResponse generateUploadUrl(UploadUrlRequest request) {
        return storagePort.generatePresignedUploadUrl(
                request.contentType(),
                request.fileSizeBytes(),
                request.inspectionId(),
                request.slot()
        );
    }
}
