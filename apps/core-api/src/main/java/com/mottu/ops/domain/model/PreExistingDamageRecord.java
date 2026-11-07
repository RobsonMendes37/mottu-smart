package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;

import java.time.Instant;

public record PreExistingDamageRecord(
        String partName,
        String description,
        Instant recordedAt,
        String photoSha256
) {
    public PreExistingDamageRecord {
        if (partName == null || partName.isBlank()) {
            throw new DomainValidationException("Part name cannot be blank");
        }
        if (recordedAt == null) {
            throw new DomainValidationException("Recorded at timestamp cannot be null");
        }
    }

    public boolean matchesPart(String reportedPart) {
        if (reportedPart == null) return false;
        return this.partName.equalsIgnoreCase(reportedPart.trim());
    }
}
