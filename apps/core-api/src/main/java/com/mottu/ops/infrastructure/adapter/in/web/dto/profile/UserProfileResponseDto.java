package com.mottu.ops.infrastructure.adapter.in.web.dto.profile;

import com.mottu.ops.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponseDto(
        UUID userId,
        UserRole role,
        String fullName,
        String cpf,
        String phone,
        String email,
        String preferredUnit,
        String pixKey,
        String cnhStatus,
        String employeeCode,
        String roleTitle,
        String shiftLabel,
        String activePlate,
        String contractCode,
        String passwordSha256Fingerprint,
        Instant updatedAt
) {}

