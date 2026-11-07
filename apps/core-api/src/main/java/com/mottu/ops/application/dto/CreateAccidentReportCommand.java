package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.VehicleDrivability;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAccidentReportCommand(
        @NotBlank(message = "videoObjectKey é obrigatório")
        String videoObjectKey,

        @NotNull(message = "videoDurationSeconds é obrigatório")
        @Min(value = 1, message = "videoDurationSeconds deve ser de pelo menos 1 segundo")
        @Max(value = 30, message = "videoDurationSeconds não pode exceder 30 segundos")
        Integer videoDurationSeconds,

        @NotNull(message = "videoSizeBytes é obrigatório")
        @Min(value = 1, message = "videoSizeBytes deve ser maior que zero")
        @Max(value = 31457280, message = "videoSizeBytes não pode exceder 30 MB")
        Long videoSizeBytes,

        List<String> photoObjectKeys,

        boolean hasInjuries,

        boolean involvesThirdParty,

        @Size(max = 60, message = "policeReportNumber deve ter no máximo 60 caracteres")
        String policeReportNumber,

        @NotNull(message = "drivability é obrigatório")
        VehicleDrivability drivability,

        @NotNull(message = "reportedOdometerKm é obrigatório")
        @Min(value = 0, message = "reportedOdometerKm deve ser não-negativo")
        Integer reportedOdometerKm,

        @Size(max = 500, message = "riderNotes deve ter no máximo 500 caracteres")
        String riderNotes,

        @Size(max = 500, message = "riderToMechanicNotes deve ter no máximo 500 caracteres")
        String riderToMechanicNotes
) {
}

