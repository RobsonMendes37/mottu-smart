package com.mottu.ops.application.dto;

public record AnalyticsSummaryResponseDto(
        long activeMotorcyclesCount,
        double autoResolvedPercentage,
        long currentHumanQueueSize,
        double estimatedMechanicHoursSaved,
        String lookerStudioEmbedUrl
) {
}
