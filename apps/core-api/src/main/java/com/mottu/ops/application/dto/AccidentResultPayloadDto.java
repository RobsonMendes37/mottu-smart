package com.mottu.ops.application.dto;

import java.util.List;
import java.util.UUID;

public record AccidentResultPayloadDto(
        UUID accidentReportId,
        UUID correlationId,
        String outcome, // "SUCCESS" or "AI_UNAVAILABLE"
        Double confidence,
        Boolean structuralRiskAlert,
        String aiTechnicalSummary,
        List<AccidentDeduplicatedPartDto> deduplicatedParts
) {
}

