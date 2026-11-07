package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.ReportedOriginContext;

import java.util.UUID;

public record TriageJobPayloadDto(
        UUID inspectionId,
        UUID correlationId,
        String photoObjectKey,
        MotorcycleModel motorcycleModel,
        ReportedOriginContext reportedContext,
        double hoursSincePickup,
        int kmDrivenSincePickup,
        String riderNotes
) {
}
