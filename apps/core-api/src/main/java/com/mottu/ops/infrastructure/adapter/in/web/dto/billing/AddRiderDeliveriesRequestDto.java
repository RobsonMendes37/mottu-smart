package com.mottu.ops.infrastructure.adapter.in.web.dto.billing;

import java.math.BigDecimal;

public record AddRiderDeliveriesRequestDto(
        int completedCount,
        BigDecimal earningsBrl
) {}

