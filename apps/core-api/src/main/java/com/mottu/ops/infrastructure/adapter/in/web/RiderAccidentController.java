package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.RiderAccidentUseCase;
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
@RequestMapping("/api/v1/rider/accidents")
@PreAuthorize("hasRole('RIDER')")
public class RiderAccidentController {

    private final RiderAccidentUseCase riderAccidentUseCase;
    private final SpringDataUserRepository userRepository;

    public RiderAccidentController(
            RiderAccidentUseCase riderAccidentUseCase,
            SpringDataUserRepository userRepository
    ) {
        this.riderAccidentUseCase = riderAccidentUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload-video-url")
    public ResponseEntity<AccidentVideoUploadUrlResponse> generateVideoUploadUrl(
            @Valid @RequestBody AccidentVideoUploadUrlRequest request
    ) {
        return ResponseEntity.ok(riderAccidentUseCase.generateVideoUploadUrl(request));
    }

    @PostMapping("/upload-photo-url")
    public ResponseEntity<AccidentPhotoUploadUrlResponse> generatePhotoUploadUrl(
            @Valid @RequestBody AccidentPhotoUploadUrlRequest request
    ) {
        return ResponseEntity.ok(riderAccidentUseCase.generatePhotoUploadUrl(request));
    }

    @PostMapping
    public ResponseEntity<CreateAccidentReportResponse> createAccidentReport(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAccidentReportCommand command,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        CreateAccidentReportResponse response = riderAccidentUseCase.createAccidentReport(riderId, idempotencyKey, command);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiderAccidentReportDetailsResponse> getAccidentDetails(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(riderAccidentUseCase.getAccidentDetails(riderId, id));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream(@PathVariable UUID id) {
        return riderAccidentUseCase.subscribeLiveStatus(id);
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

