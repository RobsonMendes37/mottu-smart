package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AppointmentShift;
import com.mottu.ops.domain.enums.InspectionStatus;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAppointmentResponse(
        UUID appointmentId,
        InspectionStatus inspectionStatus,
        String yardName,
        LocalDate scheduledDate,
        AppointmentShift shift
) {
}
