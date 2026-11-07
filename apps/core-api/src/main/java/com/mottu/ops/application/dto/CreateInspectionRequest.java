package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInspectionRequest(
        @NotBlank(message = "photoObjectKey é obrigatório")
        String photoObjectKey,

        @NotNull(message = "reportedContext é obrigatório")
        ReportedOriginContext reportedContext,

        @NotNull(message = "safetyRisk é obrigatório")
        SafetyRiskLevel safetyRisk,

        @Min(value = 0, message = "reportedOdometerKm não pode ser negativo")
        int reportedOdometerKm,

        @Size(max = 300, message = "riderNotes não pode exceder 300 caracteres")
        String riderNotes
) {
}
