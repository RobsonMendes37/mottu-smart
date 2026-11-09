package com.mottu.ops.infrastructure.adapter.in.web.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record DetailedAnalyticsResponseDto(
        List<YardSnapshotDto> yardSnapshots,
        List<DailyVolumeBarDto> dailyVolumeBars,
        List<VerdictBreakdownItemDto> verdictBreakdown,
        List<FleetModelBreakdownDto> fleetModelBreakdown,
        List<TopAuditedPartDto> topAuditedParts
) {}

