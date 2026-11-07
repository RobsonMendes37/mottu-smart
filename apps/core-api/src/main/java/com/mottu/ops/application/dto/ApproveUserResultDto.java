package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccountStatus;
import java.util.UUID;

public record ApproveUserResultDto(
        UUID userId,
        AccountStatus status,
        UUID contractId
) {}
