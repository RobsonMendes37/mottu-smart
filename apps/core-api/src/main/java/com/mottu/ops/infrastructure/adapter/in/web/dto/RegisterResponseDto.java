package com.mottu.ops.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public record RegisterResponseDto(
        UUID userId,
        String status,
        String message
) {}
