package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AppointmentShift;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID yardId,
        @NotNull LocalDate scheduledDate,
        @NotNull AppointmentShift shift
) {
}
