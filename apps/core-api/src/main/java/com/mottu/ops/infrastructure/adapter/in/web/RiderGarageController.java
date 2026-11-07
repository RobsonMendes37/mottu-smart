package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.RiderGarageResponse;
import com.mottu.ops.application.dto.UploadUrlRequest;
import com.mottu.ops.application.dto.UploadUrlResponse;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.GenerateUploadUrlUseCase;
import com.mottu.ops.application.port.in.GetRiderGarageUseCase;
import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.domain.model.User;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rider")
@PreAuthorize("hasRole('RIDER')")
public class RiderGarageController {

    private final GetRiderGarageUseCase getRiderGarageUseCase;
    private final GenerateUploadUrlUseCase generateUploadUrlUseCase;
    private final UserRepositoryPort userRepositoryPort;

    public RiderGarageController(
            GetRiderGarageUseCase getRiderGarageUseCase,
            GenerateUploadUrlUseCase generateUploadUrlUseCase,
            UserRepositoryPort userRepositoryPort
    ) {
        this.getRiderGarageUseCase = getRiderGarageUseCase;
        this.generateUploadUrlUseCase = generateUploadUrlUseCase;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping("/garage")
    public ResponseEntity<RiderGarageResponse> getGarage(Authentication authentication) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(getRiderGarageUseCase.getGarageForRider(riderId));
    }

    @PostMapping({"/inspections/upload-url", "/upload-url"})
    public ResponseEntity<UploadUrlResponse> generateUploadUrl(
            @Valid @RequestBody UploadUrlRequest request
    ) {
        return ResponseEntity.ok(generateUploadUrlUseCase.generateUploadUrl(request));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new InvalidCredentialsException("Usuário não autenticado");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            return userPrincipal.userId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return userRepositoryPort.findByEmail(authentication.getName())
                    .map(User::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + authentication.getName()));
        }
    }
}
