package com.mottu.ops.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record PendingUserResponseDto(
        UUID id,
        String fullName,
        String cpf,
        String phone,
        String email,
        String role,
        Instant createdAt
) {}
