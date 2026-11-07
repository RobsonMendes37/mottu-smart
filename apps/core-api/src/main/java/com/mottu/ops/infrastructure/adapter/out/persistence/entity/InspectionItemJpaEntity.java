package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inspection_items")
public class InspectionItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Column(name = "catalog_part_id")
    private UUID catalogPartId;

    @Column(name = "part_name_snapshot", nullable = false, length = 180)
    private String partNameSnapshot;

    @Column(name = "unit_price_snapshot_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshotBrl;

    @Column(name = "added_by", nullable = false, length = 15)
    private String addedBy; // 'AI' or 'MECHANIC'

    public InspectionItemJpaEntity() {
    }

    public InspectionItemJpaEntity(
            UUID id,
            UUID inspectionId,
            UUID catalogPartId,
            String partNameSnapshot,
            BigDecimal unitPriceSnapshotBrl,
            String addedBy
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.inspectionId = inspectionId;
        this.catalogPartId = catalogPartId;
        this.partNameSnapshot = partNameSnapshot;
        this.unitPriceSnapshotBrl = unitPriceSnapshotBrl;
        this.addedBy = addedBy;
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

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }
}
