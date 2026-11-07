package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accident_report_items")
public class AccidentReportItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "accident_report_id", nullable = false)
    private UUID accidentReportId;

    @Column(name = "catalog_part_id")
    private UUID catalogPartId;

    @Column(name = "part_name_snapshot", nullable = false, length = 180)
    private String partNameSnapshot;

    @Column(name = "unit_price_snapshot_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshotBrl;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "video_timestamp_seconds")
    private Integer videoTimestampSeconds;

    @Column(name = "confirmed_by_mechanic", nullable = false)
    private boolean confirmedByMechanic;

    @Column(name = "added_by", nullable = false, length = 15)
    private String addedBy; // 'AI' or 'MECHANIC'

    public AccidentReportItemJpaEntity() {
    }

    public AccidentReportItemJpaEntity(
            UUID id,
            UUID accidentReportId,
            UUID catalogPartId,
            String partNameSnapshot,
            BigDecimal unitPriceSnapshotBrl,
            int quantity,
            Integer videoTimestampSeconds,
            boolean confirmedByMechanic,
            String addedBy
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.accidentReportId = accidentReportId;
        this.catalogPartId = catalogPartId;
        this.partNameSnapshot = partNameSnapshot;
        this.unitPriceSnapshotBrl = unitPriceSnapshotBrl;
        this.quantity = quantity;
        this.videoTimestampSeconds = videoTimestampSeconds;
        this.confirmedByMechanic = confirmedByMechanic;
        this.addedBy = addedBy;
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

    public UUID getCatalogPartId() {
        return catalogPartId;
    }

    public void setCatalogPartId(UUID catalogPartId) {
        this.catalogPartId = catalogPartId;
    }

    public String getPartNameSnapshot() {
        return partNameSnapshot;
    }

    public void setPartNameSnapshot(String partNameSnapshot) {
        this.partNameSnapshot = partNameSnapshot;
    }

    public BigDecimal getUnitPriceSnapshotBrl() {
        return unitPriceSnapshotBrl;
    }

    public void setUnitPriceSnapshotBrl(BigDecimal unitPriceSnapshotBrl) {
        this.unitPriceSnapshotBrl = unitPriceSnapshotBrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getVideoTimestampSeconds() {
        return videoTimestampSeconds;
    }

    public void setVideoTimestampSeconds(Integer videoTimestampSeconds) {
        this.videoTimestampSeconds = videoTimestampSeconds;
    }

    public boolean isConfirmedByMechanic() {
        return confirmedByMechanic;
    }

    public void setConfirmedByMechanic(boolean confirmedByMechanic) {
        this.confirmedByMechanic = confirmedByMechanic;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }
}

