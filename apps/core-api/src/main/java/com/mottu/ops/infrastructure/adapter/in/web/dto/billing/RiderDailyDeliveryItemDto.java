package com.mottu.ops.infrastructure.adapter.in.web.dto.billing;

import java.math.BigDecimal;

public record RiderDailyDeliveryItemDto(
        String dayLabel,
        int completedCount,
        BigDecimal earningsBrl,
        String onTimeRate
) {}

