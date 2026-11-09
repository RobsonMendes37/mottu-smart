package com.mottu.ops.infrastructure.adapter.in.web.dto.profile;

public record UpdateProfileRequestDto(
        String fullName,
        String phone,
        String email,
        String pixKey,
        String preferredUnit,
        String roleTitle
) {}

