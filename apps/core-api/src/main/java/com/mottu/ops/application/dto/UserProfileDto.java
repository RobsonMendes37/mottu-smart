package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import java.util.UUID;

public record UserProfileDto(
        UUID userId,
        String fullName,
        String email,
        String cpf,
        String phone,
        UserRole role,
        AccountStatus status
) {}
