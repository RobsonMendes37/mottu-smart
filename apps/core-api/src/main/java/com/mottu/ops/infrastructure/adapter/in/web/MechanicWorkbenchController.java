package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.MechanicInspectionDetailsResponse;
import com.mottu.ops.application.dto.MechanicQueueItemDto;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.ResolveInspectionRequest;
import com.mottu.ops.application.dto.ResolveInspectionResponse;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.MechanicWorkbenchUseCase;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mechanic")
@PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
public class MechanicWorkbenchController {

    private final MechanicWorkbenchUseCase mechanicWorkbenchUseCase;
    private final SpringDataUserRepository userRepository;

    public MechanicWorkbenchController(
            MechanicWorkbenchUseCase mechanicWorkbenchUseCase,
            SpringDataUserRepository userRepository
    ) {
        this.mechanicWorkbenchUseCase = mechanicWorkbenchUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping("/queue")
    public ResponseEntity<PageResponse<MechanicQueueItemDto>> getQueue(
            @RequestParam(required = false) QueuePriorityReason reason,
            @RequestParam(required = false) String plate,
            @PageableDefault(size = 20, sort = {"priorityWeight", "createdAt"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(mechanicWorkbenchUseCase.getQueue(reason, plate, pageable));
    }

    @GetMapping("/inspections/{id}")
    public ResponseEntity<MechanicInspectionDetailsResponse> getInspectionDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(mechanicWorkbenchUseCase.getDetails(id));
    }

    @PostMapping("/inspections/{id}/resolve")
    public ResponseEntity<ResolveInspectionResponse> resolveInspection(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveInspectionRequest request,
            Authentication authentication
    ) {
        UUID mechanicId = extractUserId(authentication);
        return ResponseEntity.ok(mechanicWorkbenchUseCase.resolve(mechanicId, id, request));
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
