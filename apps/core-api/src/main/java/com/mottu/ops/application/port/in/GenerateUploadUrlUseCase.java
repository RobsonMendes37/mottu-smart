package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.UploadUrlRequest;
import com.mottu.ops.application.dto.UploadUrlResponse;

public interface GenerateUploadUrlUseCase {
    UploadUrlResponse generateUploadUrl(UploadUrlRequest request);
}
