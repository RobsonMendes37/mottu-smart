package com.mottu.ops.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InspectionItemDetailDto(
        UUID catalogPartId,
        String partName,
        BigDecimal unitPriceBrl,
        String addedBy
) {
}
