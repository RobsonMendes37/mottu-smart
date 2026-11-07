package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;

import java.util.UUID;

public record ContestInspectionResponse(
        UUID inspectionId,
        InspectionStatus status,
        QueuePriorityReason priorityReason
) {
}
