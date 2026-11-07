package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TriageResultPayloadDto(
        UUID inspectionId,
        UUID correlationId,
        String outcome, // "SUCCESS" or "AI_UNAVAILABLE"
        Boolean isValidMotorcyclePhoto,
        BigDecimal confidence,
        DamageVerdict suggestedVerdict,
        List<MatchedPartDto> matchedParts,
        String technicalSummary,
        String customerFriendlyMessage
) {
}
