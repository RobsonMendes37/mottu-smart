package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AppointmentShift;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AppointmentDetailsDto(
        UUID appointmentId,
        UUID yardId,
        String yardName,
        LocalDate scheduledDate,
        AppointmentShift shift,
        Instant createdAt
) {
}
