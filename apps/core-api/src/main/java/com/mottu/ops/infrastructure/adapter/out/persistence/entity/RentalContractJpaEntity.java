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

    @Column(name = "contract_code", length = 30)
    private String contractCode;

    @Column(name = "weekly_plan_brl", precision = 10, scale = 2)
    private java.math.BigDecimal weeklyPlanBrl = new java.math.BigDecimal("196.00");

    @Column(name = "discount_brl", precision = 10, scale = 2)
    private java.math.BigDecimal discountBrl = new java.math.BigDecimal("15.00");

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "PENDING_PIX";

    @Column(name = "due_date_label", length = 80)
    private String dueDateLabel = "Vence segunda-feira • 18:00";

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "pickup_photos_json", columnDefinition = "jsonb")
    private String pickupPhotosJson;

    @Column(name = "pickup_validated_by_rider", nullable = false)
    private boolean pickupValidatedByRider = false;

    @Column(name = "pickup_validated_at")
    private Instant pickupValidatedAt;

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

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }

    public java.math.BigDecimal getWeeklyPlanBrl() {
        return weeklyPlanBrl != null ? weeklyPlanBrl : new java.math.BigDecimal("196.00");
    }

    public void setWeeklyPlanBrl(java.math.BigDecimal weeklyPlanBrl) {
        this.weeklyPlanBrl = weeklyPlanBrl;
    }

    public java.math.BigDecimal getDiscountBrl() {
        return discountBrl != null ? discountBrl : new java.math.BigDecimal("15.00");
    }

    public void setDiscountBrl(java.math.BigDecimal discountBrl) {
        this.discountBrl = discountBrl;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : "PENDING_PIX";
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getDueDateLabel() {
        return dueDateLabel != null ? dueDateLabel : "Vence segunda-feira • 18:00";
    }

    public void setDueDateLabel(String dueDateLabel) {
        this.dueDateLabel = dueDateLabel;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getPickupPhotosJson() {
        return pickupPhotosJson;
    }

    public void setPickupPhotosJson(String pickupPhotosJson) {
        this.pickupPhotosJson = pickupPhotosJson;
    }

    public boolean isPickupValidatedByRider() {
        return pickupValidatedByRider;
    }

    public void setPickupValidatedByRider(boolean pickupValidatedByRider) {
        this.pickupValidatedByRider = pickupValidatedByRider;
    }

    public Instant getPickupValidatedAt() {
        return pickupValidatedAt;
    }

    public void setPickupValidatedAt(Instant pickupValidatedAt) {
        this.pickupValidatedAt = pickupValidatedAt;
    }
}
