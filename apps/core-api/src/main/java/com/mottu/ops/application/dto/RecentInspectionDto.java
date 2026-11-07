package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecentInspectionDto(
        UUID id,
        InspectionStatus status,
        DamageVerdict finalVerdict,
        BigDecimal totalEstimatedBrl,
        Instant createdAt
) {
}
