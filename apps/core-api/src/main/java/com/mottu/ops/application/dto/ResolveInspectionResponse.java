package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ResolveInspectionResponse(
        UUID inspectionId,
        InspectionStatus status,
        DamageVerdict finalVerdict,
        BigDecimal totalEstimatedBrl,
        String customerFeedbackText
) {
}
