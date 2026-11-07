package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.MotorcycleStatus;

import java.util.UUID;

public record CreateAccidentReportResponse(
        UUID accidentReportId,
        AccidentReportStatus status,
        MotorcycleStatus motorcycleStatus
) {
}

