package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.ManagerSettingsDto;
import com.mottu.ops.application.port.in.ManagerSettingsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/settings")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerSettingsController {

    private final ManagerSettingsUseCase managerSettingsUseCase;

    public ManagerSettingsController(ManagerSettingsUseCase managerSettingsUseCase) {
        this.managerSettingsUseCase = managerSettingsUseCase;
    }

    @GetMapping
    public ResponseEntity<ManagerSettingsDto> getSettings() {
        return ResponseEntity.ok(managerSettingsUseCase.getSettings());
    }

    @PatchMapping
    public ResponseEntity<ManagerSettingsDto> updateSettings(@RequestBody ManagerSettingsDto request) {
        return ResponseEntity.ok(managerSettingsUseCase.updateSettings(request));
    }
}
