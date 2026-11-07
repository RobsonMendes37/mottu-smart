package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.RiderInspectionUseCase;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rider")
@PreAuthorize("hasRole('RIDER')")
public class RiderInspectionController {

    private final RiderInspectionUseCase riderInspectionUseCase;
    private final SpringDataUserRepository userRepository;

    public RiderInspectionController(
            RiderInspectionUseCase riderInspectionUseCase,
            SpringDataUserRepository userRepository
    ) {
        this.riderInspectionUseCase = riderInspectionUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping("/inspections")
    public ResponseEntity<CreateInspectionResponse> createInspection(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateInspectionRequest request,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        CreateInspectionResponse response = riderInspectionUseCase.createInspection(riderId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/inspections/{id}")
    public ResponseEntity<RiderInspectionDetailsResponse> getInspectionDetails(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(riderInspectionUseCase.getInspectionDetails(riderId, id));
    }

    @GetMapping(value = "/inspections/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream(@PathVariable UUID id) {
        return riderInspectionUseCase.subscribeLiveStatus(id);
    }

    @PostMapping("/inspections/{id}/contest")
    public ResponseEntity<ContestInspectionResponse> contestInspection(
            @PathVariable UUID id,
            @Valid @RequestBody ContestInspectionRequest request,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(riderInspectionUseCase.contestInspection(riderId, id, request));
    }

    @PostMapping("/inspections/{id}/appointment")
    public ResponseEntity<CreateAppointmentResponse> createAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        CreateAppointmentResponse response = riderInspectionUseCase.createAppointment(riderId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            return userRepository.findByEmail(authentication.getName())
                    .map(UserJpaEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + authentication.getName()));
        }
    }
}
