package com.mottu.ops.application.dto;

public record UploadUrlResponse(
        String uploadUrl,
        String photoObjectKey,
        int expiresInSeconds
) {
}
