package com.mottu.ops.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public record ApproveUserResponseDto(
        UUID userId,
        String status,
        UUID contractId
) {}
