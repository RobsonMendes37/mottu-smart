package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.DamageVerdict;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ResolveInspectionRequest(
        @NotNull(message = "finalVerdict é obrigatório")
        DamageVerdict finalVerdict,

        List<UUID> catalogPartIds,

        String mechanicNotes
) {
}
