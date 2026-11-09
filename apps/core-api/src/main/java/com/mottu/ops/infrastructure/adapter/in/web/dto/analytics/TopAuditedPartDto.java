package com.mottu.ops.infrastructure.adapter.in.web.dto.analytics;

import java.math.BigDecimal;

public record TopAuditedPartDto(
        String sku,
        String partName,
        String modelName,
        long occurrences,
        String wearType,
        int stockQuantity,
        int minStockQuantity,
        boolean isCritical,
        String aisleBin,
        BigDecimal unitPriceBrl
) {}

