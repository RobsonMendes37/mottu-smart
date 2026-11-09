package com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic;

import com.mottu.ops.domain.enums.ServiceOrderStatus;
import java.util.UUID;

public record ServiceOrderResponseDto(
        UUID id,
        String code,
        String plate,
        String model,
        String bay,
        String serviceTitle,
        String originLabel,
        int slaMinutes,
        ServiceOrderStatus status
) {}

