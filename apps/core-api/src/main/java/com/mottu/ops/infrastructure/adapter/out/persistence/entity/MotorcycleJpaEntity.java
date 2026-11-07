package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "motorcycles")
public class MotorcycleJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String plate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotorcycleModel model;

    @Column(name = "yard_id", nullable = false)
    private UUID yardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MotorcycleStatus status;

    @Column(name = "current_odometer_km", nullable = false)
    private int currentOdometerKm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MotorcycleJpaEntity() {
    }

    public MotorcycleJpaEntity(
            UUID id,
            String plate,
            MotorcycleModel model,
            UUID yardId,
            MotorcycleStatus status,
            int currentOdometerKm,
            Instant createdAt
    ) {
        this.id = id;
        this.plate = plate;
        this.model = model;
        this.yardId = yardId;
        this.status = status;
        this.currentOdometerKm = currentOdometerKm;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public MotorcycleModel getModel() {
        return model;
    }

    public void setModel(MotorcycleModel model) {
        this.model = model;
    }

    public UUID getYardId() {
        return yardId;
    }

    public void setYardId(UUID yardId) {
        this.yardId = yardId;
    }

    public MotorcycleStatus getStatus() {
        return status;
    }

    public void setStatus(MotorcycleStatus status) {
        this.status = status;
    }

    public int getCurrentOdometerKm() {
        return currentOdometerKm;
    }

    public void setCurrentOdometerKm(int currentOdometerKm) {
        this.currentOdometerKm = currentOdometerKm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
