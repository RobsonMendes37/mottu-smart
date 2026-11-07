package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.MotorcycleModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogPartResponseDto(
        UUID id,
        String vtexSku,
        MotorcycleModel motorcycleModel,
        String partName,
        String category,
        BigDecimal unitPriceBrl,
        boolean isNaturalWear,
        boolean priceOverridden,
        String imageUrl,
        Instant updatedAt
) {
}
