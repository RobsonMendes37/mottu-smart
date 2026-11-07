package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccountStatus;
import java.util.UUID;

public record RegisterResultDto(
        UUID userId,
        AccountStatus status,
        String message
) {}
