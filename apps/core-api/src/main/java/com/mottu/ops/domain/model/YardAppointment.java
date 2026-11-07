package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.AppointmentShift;
import com.mottu.ops.domain.exception.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class YardAppointment {
    private final UUID id;
    private final UUID inspectionId;
    private final UUID yardId;
    private final LocalDate scheduledDate;
    private final AppointmentShift shift;
    private final Instant createdAt;

    public YardAppointment(UUID id, UUID inspectionId, UUID yardId, LocalDate scheduledDate, AppointmentShift shift, Instant createdAt) {
        if (inspectionId == null) {
            throw new DomainValidationException("Inspection ID cannot be null");
        }
        if (yardId == null) {
            throw new DomainValidationException("Yard ID cannot be null");
        }
        if (scheduledDate == null) {
            throw new DomainValidationException("Scheduled date cannot be null");
        }
        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new DomainValidationException("A data do agendamento não pode ser no passado");
        }
        if (shift == null) {
            throw new DomainValidationException("Appointment shift cannot be null");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.inspectionId = inspectionId;
        this.yardId = yardId;
        this.scheduledDate = scheduledDate;
        this.shift = shift;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public UUID getYardId() {
        return yardId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public AppointmentShift getShift() {
        return shift;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
