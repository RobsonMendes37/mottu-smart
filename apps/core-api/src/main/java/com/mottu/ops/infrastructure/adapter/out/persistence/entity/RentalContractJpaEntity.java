package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rental_contracts")
public class RentalContractJpaEntity {

    @Id
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "motorcycle_id", nullable = false)
    private UUID motorcycleId;

    @Column(name = "picked_up_at", nullable = false)
    private Instant pickedUpAt;

    @Column(name = "initial_odometer_km", nullable = false)
    private int initialOdometerKm;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(nullable = false)
    private boolean active = true;

    public RentalContractJpaEntity() {
    }

    public RentalContractJpaEntity(
            UUID id,
            UUID riderId,
            UUID motorcycleId,
            Instant pickedUpAt,
            int initialOdometerKm,
            Instant endedAt,
            boolean active
    ) {
        this.id = id;
        this.riderId = riderId;
        this.motorcycleId = motorcycleId;
        this.pickedUpAt = pickedUpAt;
        this.initialOdometerKm = initialOdometerKm;
        this.endedAt = endedAt;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public void setRiderId(UUID riderId) {
        this.riderId = riderId;
    }

    public UUID getMotorcycleId() {
        return motorcycleId;
    }

    public void setMotorcycleId(UUID motorcycleId) {
        this.motorcycleId = motorcycleId;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public int getInitialOdometerKm() {
        return initialOdometerKm;
    }

    public void setInitialOdometerKm(int initialOdometerKm) {
        this.initialOdometerKm = initialOdometerKm;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
