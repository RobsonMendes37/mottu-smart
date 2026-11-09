package com.mottu.ops.infrastructure.adapter.in.web.dto.billing;

import java.math.BigDecimal;
import java.util.UUID;

public record RiderChargedInspectionDto(
        UUID inspectionId,
        String date,
        String partName,
        String verdict,
        BigDecimal amountBrl
) {}

