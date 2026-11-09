package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rider_daily_deliveries")
public class RiderDailyDeliveryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "day_label", nullable = false, length = 40)
    private String dayLabel;

    @Column(name = "completed_count", nullable = false)
    private int completedCount;

    @Column(name = "earnings_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal earningsBrl;

    @Column(name = "on_time_rate", nullable = false, length = 10)
    private String onTimeRate = "98%";

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate = LocalDate.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public RiderDailyDeliveryJpaEntity() {
    }

    public RiderDailyDeliveryJpaEntity(
            UUID id,
            UUID riderId,
            UUID contractId,
            String dayLabel,
            int completedCount,
            BigDecimal earningsBrl,
            String onTimeRate,
            LocalDate deliveryDate,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.riderId = riderId;
        this.contractId = contractId;
        this.dayLabel = dayLabel;
        this.completedCount = completedCount;
        this.earningsBrl = earningsBrl;
        this.onTimeRate = onTimeRate != null ? onTimeRate : "98%";
        this.deliveryDate = deliveryDate != null ? deliveryDate : LocalDate.now();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public BigDecimal getEarningsBrl() {
        return earningsBrl;
    }

    public void setEarningsBrl(BigDecimal earningsBrl) {
        this.earningsBrl = earningsBrl;
    }

    public String getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(String onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

