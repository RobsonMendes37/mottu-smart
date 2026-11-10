package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RentalContract {
    private final UUID id;
    private final UUID riderId;
    private final UUID motorcycleId;
    private final Instant pickedUpAt;
    private final int initialOdometerKm;
    private Instant endedAt;
    private boolean active;
    private String pickupPhotosJson;
    private boolean pickupValidatedByRider;
    private Instant pickupValidatedAt;

    public RentalContract(
            UUID id,
            UUID riderId,
            UUID motorcycleId,
            Instant pickedUpAt,
            int initialOdometerKm,
            Instant endedAt,
            boolean active
    ) {
        this(id, riderId, motorcycleId, pickedUpAt, initialOdometerKm, endedAt, active, null, false, null);
    }

    public RentalContract(
            UUID id,
            UUID riderId,
            UUID motorcycleId,
            Instant pickedUpAt,
            int initialOdometerKm,
            Instant endedAt,
            boolean active,
            String pickupPhotosJson,
            boolean pickupValidatedByRider,
            Instant pickupValidatedAt
    ) {
        if (riderId == null) {
            throw new DomainValidationException("Rider ID cannot be null");
        }
        if (motorcycleId == null) {
            throw new DomainValidationException("Motorcycle ID cannot be null");
        }
        if (pickedUpAt == null) {
            throw new DomainValidationException("Picked up at timestamp cannot be null");
        }
        if (initialOdometerKm < 0) {
            throw new DomainValidationException("Initial odometer cannot be negative");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.riderId = riderId;
        this.motorcycleId = motorcycleId;
        this.pickedUpAt = pickedUpAt;
        this.initialOdometerKm = initialOdometerKm;
        this.endedAt = endedAt;
        this.active = active;
        this.pickupPhotosJson = pickupPhotosJson;
        this.pickupValidatedByRider = pickupValidatedByRider;
        this.pickupValidatedAt = pickupValidatedAt;
    }

    public static RentalContract create(UUID riderId, UUID motorcycleId, Instant pickedUpAt, int initialOdometerKm) {
        return new RentalContract(UUID.randomUUID(), riderId, motorcycleId, pickedUpAt, initialOdometerKm, null, true);
    }

    public Duration durationSincePickup(Instant now) {
        if (now.isBefore(this.pickedUpAt)) {
            throw new DomainValidationException("Evaluation time cannot be before pickup time");
        }
        return Duration.between(this.pickedUpAt, now);
    }

    public double hoursSincePickup(Instant now) {
        return durationSincePickup(now).toSeconds() / 3600.0;
    }

    public int distanceDrivenKm(int currentOdometerKm) {
        if (currentOdometerKm < this.initialOdometerKm) {
            throw new DomainValidationException(
                    "Current odometer (" + currentOdometerKm + " km) cannot be less than initial contract odometer (" + this.initialOdometerKm + " km)"
            );
        }
        return currentOdometerKm - this.initialOdometerKm;
    }

    public void close(Instant returnTime, int finalOdometerKm) {
        if (!this.active) {
            throw new BusinessRuleViolationException("Contract is already closed");
        }
        if (returnTime.isBefore(this.pickedUpAt)) {
            throw new DomainValidationException("Return time cannot be before pickup time");
        }
        if (finalOdometerKm < this.initialOdometerKm) {
            throw new DomainValidationException("Final odometer cannot be less than initial contract odometer");
        }
        this.endedAt = returnTime;
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public UUID getMotorcycleId() {
        return motorcycleId;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public int getInitialOdometerKm() {
        return initialOdometerKm;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getPickupPhotosJson() {
        return pickupPhotosJson;
    }

    public boolean isPickupValidatedByRider() {
        return pickupValidatedByRider;
    }

    public Instant getPickupValidatedAt() {
        return pickupValidatedAt;
    }

    public void validatePickup(Instant validatedAt) {
        this.pickupValidatedByRider = true;
        this.pickupValidatedAt = validatedAt != null ? validatedAt : Instant.now();
    }

    public void updatePickupPhotos(String pickupPhotosJson) {
        this.pickupPhotosJson = pickupPhotosJson;
    }
}
