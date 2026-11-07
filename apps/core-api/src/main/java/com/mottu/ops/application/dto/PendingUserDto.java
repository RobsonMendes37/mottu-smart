package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record PendingUserDto(
        UUID id,
        String fullName,
        String cpf,
        String phone,
        String email,
        UserRole role,
        Instant createdAt
) {}
