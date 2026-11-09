package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.ServiceOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_orders")
public class ServiceOrderJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 25)
    private String code;

    @Column(name = "motorcycle_id", nullable = false)
    private UUID motorcycleId;

    @Column(name = "inspection_id")
    private UUID inspectionId;

    @Column(name = "yard_id", nullable = false)
    private UUID yardId;

    @Column(name = "bay_label", nullable = false, length = 50)
    private String bayLabel;

    @Column(name = "service_title", nullable = false, length = 220)
    private String serviceTitle;

    @Column(name = "origin_label", nullable = false, length = 140)
    private String originLabel;

    @Column(name = "sla_minutes", nullable = false)
    private int slaMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ServiceOrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ServiceOrderJpaEntity() {
    }

    public ServiceOrderJpaEntity(
            UUID id,
            String code,
            UUID motorcycleId,
            UUID inspectionId,
            UUID yardId,
            String bayLabel,
            String serviceTitle,
            String originLabel,
            int slaMinutes,
            ServiceOrderStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.code = code;
        this.motorcycleId = motorcycleId;
        this.inspectionId = inspectionId;
        this.yardId = yardId;
        this.bayLabel = bayLabel;
        this.serviceTitle = serviceTitle;
        this.originLabel = originLabel;
        this.slaMinutes = slaMinutes;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UUID getMotorcycleId() {
        return motorcycleId;
    }

    public void setMotorcycleId(UUID motorcycleId) {
        this.motorcycleId = motorcycleId;
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

    public String getBayLabel() {
        return bayLabel;
    }

    public void setBayLabel(String bayLabel) {
        this.bayLabel = bayLabel;
    }

    public String getServiceTitle() {
        return serviceTitle;
    }

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public String getOriginLabel() {
        return originLabel;
    }

    public void setOriginLabel(String originLabel) {
        this.originLabel = originLabel;
    }

    public int getSlaMinutes() {
        return slaMinutes;
    }

    public void setSlaMinutes(int slaMinutes) {
        this.slaMinutes = slaMinutes;
    }

    public ServiceOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceOrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

