package com.mottu.ops.application.dto;

import java.math.BigDecimal;

public record UpdateCatalogPartRequest(
        BigDecimal unitPriceBrl,
        Boolean isNaturalWear
) {
}
