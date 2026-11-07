package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccountStatus;
import java.util.UUID;

public record RejectUserResultDto(
        UUID userId,
        AccountStatus status
) {}
