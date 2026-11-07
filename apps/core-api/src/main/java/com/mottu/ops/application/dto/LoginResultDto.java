package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.UserRole;
import java.util.UUID;

public record LoginResultDto(
        String accessToken,
        UUID userId,
        UserRole role,
        String fullName
) {}
