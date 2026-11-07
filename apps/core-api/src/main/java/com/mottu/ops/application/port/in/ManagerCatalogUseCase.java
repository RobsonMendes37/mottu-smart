package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.CatalogPartResponseDto;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.SyncCatalogResponseDto;
import com.mottu.ops.application.dto.UpdateCatalogPartRequest;
import com.mottu.ops.domain.enums.MotorcycleModel;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ManagerCatalogUseCase {
    SyncCatalogResponseDto syncMottuStore();
    PageResponse<CatalogPartResponseDto> getParts(MotorcycleModel model, String search, Pageable pageable);
    CatalogPartResponseDto updatePart(UUID id, UpdateCatalogPartRequest request);
}
