package com.mottu.ops.application.dto;

public record AccidentVideoUploadUrlResponse(
        String uploadUrl,
        String videoObjectKey,
        int expiresInSeconds
) {
}

