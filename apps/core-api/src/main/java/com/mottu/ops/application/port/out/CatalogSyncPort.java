package com.mottu.ops.application.port.out;

import com.mottu.ops.application.dto.SyncCatalogResponseDto;

public interface CatalogSyncPort {
    SyncCatalogResponseDto syncCatalog();
}
