package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;

import java.util.UUID;

public class InspectionItem {
    private final UUID id;
    private final UUID inspectionId;
    private final UUID catalogPartId;
    private final String partNameSnapshot;
    private final Money unitPriceSnapshot;
    private final String addedBy; // "AI" or "MECHANIC"

    public InspectionItem(
            UUID id,
            UUID inspectionId,
            UUID catalogPartId,
            String partNameSnapshot,
            Money unitPriceSnapshot,
            String addedBy
    ) {
        if (inspectionId == null) {
            throw new DomainValidationException("Inspection ID cannot be null");
        }
        if (partNameSnapshot == null || partNameSnapshot.isBlank()) {
            throw new DomainValidationException("Part name snapshot cannot be null or blank");
        }
        if (unitPriceSnapshot == null) {
            throw new DomainValidationException("Unit price snapshot cannot be null");
        }
        if (!"AI".equals(addedBy) && !"MECHANIC".equals(addedBy)) {
            throw new DomainValidationException("addedBy must be either 'AI' or 'MECHANIC'");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.inspectionId = inspectionId;
        this.catalogPartId = catalogPartId;
        this.partNameSnapshot = partNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.addedBy = addedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public UUID getCatalogPartId() {
        return catalogPartId;
    }

    public String getPartNameSnapshot() {
        return partNameSnapshot;
    }

    public Money getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public String getAddedBy() {
        return addedBy;
    }
}
