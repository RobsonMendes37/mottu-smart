package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccidentReportStatus;

import java.util.UUID;

public record AccidentStatusChangeEventDto(
        UUID accidentReportId,
        AccidentReportStatus status,
        String stepLabel
) {
}

