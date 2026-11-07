package com.mottu.ops.application.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ConfirmAccidentReportCommand(
        List<UUID> confirmedPartIds,
        List<UUID> additionalCatalogPartIds,
        @Size(max = 500, message = "mechanicNotes deve ter no máximo 500 caracteres")
        String mechanicNotes
) {
}

