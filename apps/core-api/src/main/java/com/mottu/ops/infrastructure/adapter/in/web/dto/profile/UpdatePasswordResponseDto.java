package com.mottu.ops.infrastructure.adapter.in.web.dto.profile;

public record UpdatePasswordResponseDto(
        boolean success,
        String message,
        String passwordSha256Fingerprint
) {}

