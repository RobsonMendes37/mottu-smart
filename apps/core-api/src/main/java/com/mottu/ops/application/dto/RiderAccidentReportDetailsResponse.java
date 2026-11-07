package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.VehicleDrivability;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiderAccidentReportDetailsResponse(
        UUID id,
        AccidentReportStatus status,
        String videoDownloadUrl,
        VehicleDrivability drivability,
        boolean hasInjuries,
        boolean structuralRiskAlert,
        Double aiConfidence,
        BigDecimal partsSubtotalBrl,
        BigDecimal laborFeeBrl,
        BigDecimal towingFeeBrl,
        BigDecimal totalEstimatedBrl,
        List<AccidentItemResponseDto> items,
        String riderToMechanicNotes,
        Instant createdAt
) {
}

