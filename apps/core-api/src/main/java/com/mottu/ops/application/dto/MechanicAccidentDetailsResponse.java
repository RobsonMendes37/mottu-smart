package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.VehicleDrivability;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MechanicAccidentDetailsResponse(
        UUID id,
        AccidentReportStatus status,
        String plate,
        MotorcycleModel model,
        String riderName,
        String riderPhone,
        String videoDownloadUrl,
        List<String> photoDownloadUrls,
        VehicleDrivability drivability,
        boolean hasInjuries,
        boolean involvesThirdParty,
        String policeReportNumber,
        int reportedOdometerKm,
        String riderNotes,
        String riderToMechanicNotes,
        Double aiConfidence,
        boolean structuralRiskAlert,
        String aiTechnicalSummary,
        BigDecimal partsSubtotalBrl,
        BigDecimal laborFeeBrl,
        BigDecimal towingFeeBrl,
        BigDecimal totalEstimatedBrl,
        List<AccidentItemResponseDto> items,
        Instant createdAt,
        Instant confirmedAt,
        String mechanicNotes
) {
}

