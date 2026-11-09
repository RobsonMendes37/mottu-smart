package com.mottu.ops.infrastructure.adapter.in.web.dto.analytics;

public record DailyVolumeBarDto(
        String dayLabel,
        long autoCount,
        long humanCount,
        long totalCount
) {}

