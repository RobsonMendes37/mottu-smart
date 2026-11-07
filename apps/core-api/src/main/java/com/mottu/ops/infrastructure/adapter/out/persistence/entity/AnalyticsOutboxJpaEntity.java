package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_outbox")
public class AnalyticsOutboxJpaEntity {

    @Id
    private UUID id;

    @Column(name = "inspection_id")
    private UUID inspectionId;

    @Column(name = "accident_report_id")
    private UUID accidentReportId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "exported", nullable = false)
    private boolean exported;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "exported_at")
    private Instant exportedAt;

    public AnalyticsOutboxJpaEntity() {
    }

    public AnalyticsOutboxJpaEntity(
            UUID id,
            UUID inspectionId,
            String payloadJson,
            boolean exported,
            Instant createdAt,
            Instant exportedAt
    ) {
        this(id, inspectionId, null, payloadJson, exported, createdAt, exportedAt);
    }

    public AnalyticsOutboxJpaEntity(
            UUID id,
            UUID inspectionId,
            UUID accidentReportId,
            String payloadJson,
            boolean exported,
            Instant createdAt,
            Instant exportedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.inspectionId = inspectionId;
        this.accidentReportId = accidentReportId;
        this.payloadJson = payloadJson;
        this.exported = exported;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.exportedAt = exportedAt;
    }

    public UUID getAccidentReportId() {
        return accidentReportId;
    }

    public void setAccidentReportId(UUID accidentReportId) {
        this.accidentReportId = accidentReportId;
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

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public boolean isExported() {
        return exported;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(Instant exportedAt) {
        this.exportedAt = exportedAt;
    }
}
