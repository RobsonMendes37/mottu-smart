package com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic;

import java.util.List;

public record MechanicOperationsResponseDto(
        List<ServiceOrderResponseDto> serviceOrders,
        List<StockItemResponseDto> stockItems
) {}

