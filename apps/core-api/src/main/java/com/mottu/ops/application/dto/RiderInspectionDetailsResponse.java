package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiderInspectionDetailsResponse(
        UUID id,
        InspectionStatus status,
        String photoDownloadUrl,
        ReportedOriginContext reportedContext,
        SafetyRiskLevel safetyRisk,
        int reportedOdometerKm,
        String riderNotes,
        DamageVerdict finalVerdict,
        BigDecimal aiConfidence,
        BigDecimal laborFeeBrl,
        BigDecimal totalEstimatedBrl,
        String customerFeedbackText,
        boolean canContest,
        boolean contestedOnce,
        List<InspectionItemDetailDto> items,
        Object appointment,
        Instant createdAt,
        Instant resolvedAt,
        String committeeEvaluationJson,
        boolean presentialInspectionRequested,
        String presentialRequestReason
) {
}
