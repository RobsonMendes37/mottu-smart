package com.mottu.ops.infrastructure.adapter.in.web.dto.analytics;

public record FleetModelBreakdownDto(
        String modelKey,
        String modelName,
        long inspectionsCount,
        double deflectionPct,
        int slaMinutes
) {}

