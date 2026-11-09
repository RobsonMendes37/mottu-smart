package com.mottu.ops.infrastructure.adapter.in.web.dto.analytics;

public record VerdictBreakdownItemDto(
        String verdict,
        String label,
        long count,
        double percentage
) {}

