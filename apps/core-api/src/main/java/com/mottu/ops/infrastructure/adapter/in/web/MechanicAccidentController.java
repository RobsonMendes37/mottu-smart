package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.MechanicAccidentUseCase;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mechanic/accidents")
@PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
public class MechanicAccidentController {

    private final MechanicAccidentUseCase mechanicAccidentUseCase;
    private final SpringDataUserRepository userRepository;

    public MechanicAccidentController(
            MechanicAccidentUseCase mechanicAccidentUseCase,
            SpringDataUserRepository userRepository
    ) {
        this.mechanicAccidentUseCase = mechanicAccidentUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<MechanicAccidentQueueItemDto>> getAccidentsQueue(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MechanicAccidentQueueItemDto> page = mechanicAccidentUseCase.getAccidentsQueue(pageable);
        return ResponseEntity.ok(PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MechanicAccidentDetailsResponse> getAccidentDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(mechanicAccidentUseCase.getAccidentDetails(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ConfirmAccidentReportResponse> confirmAccident(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmAccidentReportCommand command,
            Authentication authentication
    ) {
        UUID mechanicId = extractUserId(authentication);
        return ResponseEntity.ok(mechanicAccidentUseCase.confirmAccident(mechanicId, id, command));
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

