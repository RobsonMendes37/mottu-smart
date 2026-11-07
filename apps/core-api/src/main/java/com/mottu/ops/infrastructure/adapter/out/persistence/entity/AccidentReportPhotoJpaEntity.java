package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accident_report_photos")
public class AccidentReportPhotoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "accident_report_id", nullable = false)
    private UUID accidentReportId;

    @Column(name = "photo_object_key", nullable = false, length = 255)
    private String photoObjectKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AccidentReportPhotoJpaEntity() {
    }

    public AccidentReportPhotoJpaEntity(UUID id, UUID accidentReportId, String photoObjectKey, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.accidentReportId = accidentReportId;
        this.photoObjectKey = photoObjectKey;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccidentReportId() {
        return accidentReportId;
    }

    public void setAccidentReportId(UUID accidentReportId) {
        this.accidentReportId = accidentReportId;
    }

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public void setPhotoObjectKey(String photoObjectKey) {
        this.photoObjectKey = photoObjectKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

