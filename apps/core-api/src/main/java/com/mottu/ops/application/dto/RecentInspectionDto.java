package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecentInspectionDto(
        UUID id,
        InspectionStatus status,
        DamageVerdict finalVerdict,
        BigDecimal totalEstimatedBrl,
        Instant createdAt,
        int itemsCount,
        List<String> partNamesSummary
) {
    public RecentInspectionDto(
            UUID id,
            InspectionStatus status,
            DamageVerdict finalVerdict,
            BigDecimal totalEstimatedBrl,
            Instant createdAt
    ) {
        this(id, status, finalVerdict, totalEstimatedBrl, createdAt, 0, List.of());
    }
}
