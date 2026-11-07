package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Motorcycle {
    private final UUID id;
    private final String plate;
    private final MotorcycleModel model;
    private UUID yardId;
    private MotorcycleStatus status;
    private int currentOdometerKm;
    private final Instant createdAt;

    public Motorcycle(
            UUID id,
            String plate,
            MotorcycleModel model,
            UUID yardId,
            MotorcycleStatus status,
            int currentOdometerKm,
            Instant createdAt
    ) {
        if (plate == null || plate.isBlank()) {
            throw new DomainValidationException("Motorcycle plate cannot be null or blank");
        }
        if (model == null) {
            throw new DomainValidationException("Motorcycle model cannot be null");
        }
        if (yardId == null) {
            throw new DomainValidationException("Yard ID cannot be null");
        }
        if (status == null) {
            throw new DomainValidationException("Motorcycle status cannot be null");
        }
        if (currentOdometerKm < 0) {
            throw new DomainValidationException("Odometer cannot be negative");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.plate = plate.toUpperCase().trim();
        this.model = model;
        this.yardId = yardId;
        this.status = status;
        this.currentOdometerKm = currentOdometerKm;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void updateOdometer(int newOdometerKm) {
        if (newOdometerKm < this.currentOdometerKm) {
            throw new DomainValidationException(
                    "New odometer (" + newOdometerKm + " km) cannot be less than current odometer (" + this.currentOdometerKm + " km)"
            );
        }
        this.currentOdometerKm = newOdometerKm;
    }

    public void rent() {
        if (this.status != MotorcycleStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Only AVAILABLE motorcycles can be rented. Current status: " + this.status);
        }
        this.status = MotorcycleStatus.RENTED;
    }

    public void returnToYard(UUID newYardId, int finalOdometerKm) {
        if (this.status != MotorcycleStatus.RENTED) {
            throw new BusinessRuleViolationException("Only RENTED motorcycles can be returned to yard");
        }
        updateOdometer(finalOdometerKm);
        this.yardId = Objects.requireNonNull(newYardId, "New yard ID cannot be null");
        this.status = MotorcycleStatus.AVAILABLE;
    }

    public void sendToMaintenance() {
        this.status = MotorcycleStatus.IN_MAINTENANCE;
    }

    public void releaseFromMaintenance() {
        this.status = MotorcycleStatus.AVAILABLE;
    }

    public UUID getId() {
        return id;
    }

    public String getPlate() {
        return plate;
    }

    public MotorcycleModel getModel() {
        return model;
    }

    public UUID getYardId() {
        return yardId;
    }

    public MotorcycleStatus getStatus() {
        return status;
    }

    public int getCurrentOdometerKm() {
        return currentOdometerKm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
