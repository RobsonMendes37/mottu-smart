package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccidentReportStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConfirmAccidentReportResponse(
        UUID accidentReportId,
        AccidentReportStatus status,
        BigDecimal totalEstimatedBrl,
        Instant confirmedAt
) {
}

