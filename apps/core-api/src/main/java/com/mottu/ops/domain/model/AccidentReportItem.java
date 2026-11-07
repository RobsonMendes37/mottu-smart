package com.mottu.ops.domain.model;

import java.util.Objects;
import java.util.UUID;

public class AccidentReportItem {

    private final UUID id;
    private final UUID catalogPartId;
    private final String partName;
    private final Money unitPrice;
    private final int quantity;
    private final Integer videoTimestampSeconds;
    private boolean confirmedByMechanic;
    private final String addedBy; // 'AI' or 'MECHANIC'

    private AccidentReportItem(
            UUID id,
            UUID catalogPartId,
            String partName,
            Money unitPrice,
            int quantity,
            Integer videoTimestampSeconds,
            boolean confirmedByMechanic,
            String addedBy
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.catalogPartId = Objects.requireNonNull(catalogPartId, "catalogPartId cannot be null");
        if (partName == null || partName.trim().isEmpty()) {
            throw new IllegalArgumentException("partName cannot be empty");
        }
        this.partName = partName.trim();
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        this.quantity = quantity;
        this.videoTimestampSeconds = videoTimestampSeconds;
        this.confirmedByMechanic = confirmedByMechanic;
        this.addedBy = Objects.requireNonNull(addedBy, "addedBy cannot be null");
    }

    public static AccidentReportItem createAiItem(
            UUID catalogPartId,
            String partName,
            Money unitPrice,
            int quantity,
            Integer videoTimestampSeconds
    ) {
        return new AccidentReportItem(
                UUID.randomUUID(),
                catalogPartId,
                partName,
                unitPrice,
                quantity,
                videoTimestampSeconds,
                true,
                "AI"
        );
    }

    public static AccidentReportItem createMechanicItem(
            UUID catalogPartId,
            String partName,
            Money unitPrice,
            int quantity
    ) {
        return new AccidentReportItem(
                UUID.randomUUID(),
                catalogPartId,
                partName,
                unitPrice,
                quantity,
                null,
                true,
                "MECHANIC"
        );
    }

    public static AccidentReportItem reconstitute(
            UUID id,
            UUID catalogPartId,
            String partName,
            Money unitPrice,
            int quantity,
            Integer videoTimestampSeconds,
            boolean confirmedByMechanic,
            String addedBy
    ) {
        return new AccidentReportItem(
                id,
                catalogPartId,
                partName,
                unitPrice,
                quantity,
                videoTimestampSeconds,
                confirmedByMechanic,
                addedBy
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getCatalogPartId() {
        return catalogPartId;
    }

    public String getPartName() {
        return partName;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public Integer getVideoTimestampSeconds() {
        return videoTimestampSeconds;
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
}

