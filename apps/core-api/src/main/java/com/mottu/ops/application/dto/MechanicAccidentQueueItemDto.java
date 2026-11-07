package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.VehicleDrivability;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MechanicAccidentQueueItemDto(
        UUID id,
        String plate,
        MotorcycleModel model,
        String riderName,
        VehicleDrivability drivability,
        boolean hasInjuries,
        BigDecimal totalEstimatedBrl,
        int itemCount,
        AccidentReportStatus status,
        Instant createdAt
) {
}

