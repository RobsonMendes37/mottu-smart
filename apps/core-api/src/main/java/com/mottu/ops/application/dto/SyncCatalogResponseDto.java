package com.mottu.ops.application.dto;

import java.time.Instant;

public record SyncCatalogResponseDto(
        int syncedCount,
        boolean usedFallback,
        Instant syncedAt,
        String message
) {
}
