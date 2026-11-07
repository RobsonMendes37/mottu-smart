package com.mottu.ops.application.dto;

public record AccidentPhotoUploadUrlResponse(
        String uploadUrl,
        String photoObjectKey,
        int expiresInSeconds
) {
}

