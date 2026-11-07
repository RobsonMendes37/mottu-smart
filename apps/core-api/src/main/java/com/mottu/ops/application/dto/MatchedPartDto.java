package com.mottu.ops.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MatchedPartDto(
        UUID catalogPartId,
        String partName,
        BigDecimal unitPriceBrl,
        Boolean isNaturalWear
) {
}
