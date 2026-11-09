package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.service.MechanicOperationsService;
import com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mechanic/operations")
@PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
public class MechanicOperationsController {

    private final MechanicOperationsService mechanicOperationsService;

    public MechanicOperationsController(MechanicOperationsService mechanicOperationsService) {
        this.mechanicOperationsService = mechanicOperationsService;
    }

    @GetMapping
    public ResponseEntity<MechanicOperationsResponseDto> getOperations() {
        return ResponseEntity.ok(mechanicOperationsService.getOperationsSummary());
    }

    @PostMapping("/orders")
    public ResponseEntity<ServiceOrderResponseDto> createOrder(@Valid @RequestBody CreateServiceOrderRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mechanicOperationsService.createServiceOrder(request));
    }

    @PatchMapping("/orders/{id}/advance")
    public ResponseEntity<ServiceOrderResponseDto> advanceOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(mechanicOperationsService.advanceOrderStatus(id));
    }

    @PatchMapping("/stock/{sku}")
    public ResponseEntity<StockItemResponseDto> updateStock(
            @PathVariable String sku,
            @RequestBody UpdateStockRequestDto request
    ) {
        return ResponseEntity.ok(mechanicOperationsService.updateStock(sku, request.delta()));
    }
}

