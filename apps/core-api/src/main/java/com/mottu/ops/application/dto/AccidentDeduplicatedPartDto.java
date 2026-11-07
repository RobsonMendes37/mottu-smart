package com.mottu.ops.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccidentDeduplicatedPartDto(
        UUID catalogPartId,
        String partName,
        BigDecimal unitPriceBrl,
        int quantity,
        Integer videoTimestampSeconds
) {
}

