package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.SafetyRiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MechanicQueueItemDto(
        UUID inspectionId,
        String plate,
        MotorcycleModel motorcycleModel,
        String riderName,
        Integer priorityWeight,
        QueuePriorityReason priorityReason,
        SafetyRiskLevel safetyRisk,
        boolean fraudSuspected,
        BigDecimal aiConfidence,
        DamageVerdict aiSuggestedVerdict,
        Instant createdAt
) {
}
