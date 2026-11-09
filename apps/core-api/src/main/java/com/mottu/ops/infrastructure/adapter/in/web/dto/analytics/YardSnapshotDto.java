package com.mottu.ops.infrastructure.adapter.in.web.dto.analytics;

public record YardSnapshotDto(
        String yardKey,
        String yardLabel,
        long totalInspections,
        long autoResolvedCount,
        long humanTriageCount,
        double deflectionRatePct,
        long activeFleetCount,
        double averageResolutionTimeMinutes
) {}

