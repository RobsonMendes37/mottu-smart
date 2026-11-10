package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MechanicInspectionDetailsResponse(
        UUID id,
        InspectionStatus status,
        String plate,
        MotorcycleModel motorcycleModel,
        String riderName,
        String riderPhone,
        Double hoursSincePickup,
        Integer kmDrivenSincePickup,
        String photoDownloadUrl,
        boolean fraudSuspected,
        String fraudReason,
        ReportedOriginContext reportedContext,
        SafetyRiskLevel safetyRisk,
        int reportedOdometerKm,
        String riderNotes,
        DamageVerdict aiSuggestedVerdict,
        BigDecimal aiConfidence,
        String aiTechnicalSummary,
        boolean contestedOnce,
        String contestationReason,
        DamageVerdict finalVerdict,
        BigDecimal laborFeeBrl,
        BigDecimal totalEstimatedBrl,
        String customerFeedbackText,
        List<InspectionItemDetailDto> items,
        Instant createdAt,
        Instant resolvedAt,
        String committeeEvaluationJson,
        boolean presentialInspectionRequested,
        String presentialRequestReason,
        String pickupPhotosJson
) {
}
