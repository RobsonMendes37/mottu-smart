package com.mottu.ops.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public record RejectUserResponseDto(
        UUID userId,
        String status
) {}
