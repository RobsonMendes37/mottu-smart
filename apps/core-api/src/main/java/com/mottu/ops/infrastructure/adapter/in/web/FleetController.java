package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.CreateMotorcycleCommand;
import com.mottu.ops.application.dto.FleetMotorcycleResponse;
import com.mottu.ops.application.dto.MotorcycleDetailsResponse;
import com.mottu.ops.application.dto.MotorcycleFilter;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.UpdateMotorcycleStatusCommand;
import com.mottu.ops.application.port.in.FleetManagementUseCase;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fleet/motorcycles")
@PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
public class FleetController {

    private final FleetManagementUseCase fleetManagementUseCase;

    public FleetController(FleetManagementUseCase fleetManagementUseCase) {
        this.fleetManagementUseCase = fleetManagementUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<FleetMotorcycleResponse>> listMotorcycles(
            @RequestParam(required = false) MotorcycleStatus status,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) UUID yardId,
            @RequestParam(required = false) MotorcycleModel model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        MotorcycleFilter filter = new MotorcycleFilter(status, plate, yardId, model);
        return ResponseEntity.ok(fleetManagementUseCase.listMotorcycles(filter, page, size));
    }

    @PostMapping
    public ResponseEntity<MotorcycleDetailsResponse> registerMotorcycle(@Valid @RequestBody CreateMotorcycleCommand command) {
        MotorcycleDetailsResponse response = fleetManagementUseCase.createMotorcycle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotorcycleDetailsResponse> getMotorcycleById(@PathVariable UUID id) {
        return ResponseEntity.ok(fleetManagementUseCase.getMotorcycleById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MotorcycleDetailsResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMotorcycleStatusCommand command
    ) {
        return ResponseEntity.ok(fleetManagementUseCase.updateMotorcycleStatus(id, command.status()));
    }
}
