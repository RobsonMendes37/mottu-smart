package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.CatalogPartResponseDto;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.SyncCatalogResponseDto;
import com.mottu.ops.application.dto.UpdateCatalogPartRequest;
import com.mottu.ops.application.port.in.ManagerCatalogUseCase;
import com.mottu.ops.domain.enums.MotorcycleModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manager/catalog")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerCatalogController {

    private final ManagerCatalogUseCase managerCatalogUseCase;

    public ManagerCatalogController(ManagerCatalogUseCase managerCatalogUseCase) {
        this.managerCatalogUseCase = managerCatalogUseCase;
    }

    @PostMapping("/sync-mottu-store")
    public ResponseEntity<SyncCatalogResponseDto> syncMottuStore() {
        return ResponseEntity.ok(managerCatalogUseCase.syncMottuStore());
    }

    @GetMapping("/parts")
    public ResponseEntity<PageResponse<CatalogPartResponseDto>> getParts(
            @RequestParam(required = false) MotorcycleModel model,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<CatalogPartResponseDto> response = managerCatalogUseCase.getParts(
                model,
                search,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/parts/{id}")
    public ResponseEntity<CatalogPartResponseDto> updatePart(
            @PathVariable UUID id,
            @RequestBody UpdateCatalogPartRequest request
    ) {
        return ResponseEntity.ok(managerCatalogUseCase.updatePart(id, request));
    }
}
