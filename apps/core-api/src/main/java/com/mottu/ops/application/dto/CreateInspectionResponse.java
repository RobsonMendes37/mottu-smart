package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.InspectionStatus;

import java.util.UUID;

public record CreateInspectionResponse(
        UUID inspectionId,
        InspectionStatus status
) {
}
