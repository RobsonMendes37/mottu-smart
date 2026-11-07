package com.mottu.ops.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public record LoginResponseDto(
        String accessToken,
        UUID userId,
        String role,
        String fullName
) {}
