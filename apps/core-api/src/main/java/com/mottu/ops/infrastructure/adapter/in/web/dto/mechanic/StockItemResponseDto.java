package com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic;

import java.math.BigDecimal;
import java.util.UUID;

public record StockItemResponseDto(
        UUID partId,
        String sku,
        String partName,
        String model,
        String aisleBin,
        int quantity,
        int minQuantity,
        BigDecimal unitPriceBrl
) {}

