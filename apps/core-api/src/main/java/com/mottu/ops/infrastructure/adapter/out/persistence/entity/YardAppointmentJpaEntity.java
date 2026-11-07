package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.AppointmentShift;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "yard_appointments")
public class YardAppointmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "inspection_id", nullable = false, unique = true)
    private UUID inspectionId;

    @Column(name = "yard_id", nullable = false)
    private UUID yardId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", nullable = false, length = 15)
    private AppointmentShift shift;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public YardAppointmentJpaEntity() {
    }

    public YardAppointmentJpaEntity(
            UUID id,
            UUID inspectionId,
            UUID yardId,
            LocalDate scheduledDate,
            AppointmentShift shift,
            Instant createdAt
    ) {
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

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(UUID inspectionId) {
        this.inspectionId = inspectionId;
    }

    public UUID getYardId() {
        return yardId;
    }

    public void setYardId(UUID yardId) {
        this.yardId = yardId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public AppointmentShift getShift() {
        return shift;
    }

    public void setShift(AppointmentShift shift) {
        this.shift = shift;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
