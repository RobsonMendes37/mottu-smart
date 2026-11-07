package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.VehicleDrivability;

import java.util.List;
import java.util.UUID;

public record AccidentJobPayloadDto(
        UUID accidentReportId,
        UUID correlationId,
        String videoObjectKey,
        List<String> photoObjectKeys,
        MotorcycleModel motorcycleModel,
        VehicleDrivability drivability,
        String riderNotes
) {
}

