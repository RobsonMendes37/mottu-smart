package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.ApproveUserCommand;
import com.mottu.ops.application.dto.ApproveUserResultDto;
import com.mottu.ops.application.dto.PendingUserDto;
import com.mottu.ops.application.dto.RejectUserCommand;
import com.mottu.ops.application.dto.RejectUserResultDto;
import com.mottu.ops.application.port.in.ManagerUserUseCase;
import com.mottu.ops.infrastructure.adapter.in.web.dto.ApproveUserRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.ApproveUserResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.PendingUserResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.RejectUserRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.RejectUserResponseDto;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manager/users")
public class ManagerUserController {

    private final ManagerUserUseCase managerUserUseCase;

    public ManagerUserController(ManagerUserUseCase managerUserUseCase) {
        this.managerUserUseCase = managerUserUseCase;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingUserResponseDto>> getPendingUsers() {
        List<PendingUserDto> pendingUsers = managerUserUseCase.getPendingUsers();
        List<PendingUserResponseDto> response = pendingUsers.stream()
                .map(u -> new PendingUserResponseDto(
                        u.id(),
                        u.fullName(),
                        u.cpf(),
                        u.phone(),
                        u.email(),
                        u.role().name(),
                        u.createdAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApproveUserResponseDto> approveUser(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveUserRequestDto request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UUID motorcycleId = request != null ? request.motorcycleId() : null;
        UUID managerId = principal != null ? principal.userId() : UUID.fromString("e0000000-0000-0000-0000-000000000001");

        ApproveUserCommand command = new ApproveUserCommand(id, managerId, motorcycleId);
        ApproveUserResultDto result = managerUserUseCase.approveUser(command);

        return ResponseEntity.ok(
                new ApproveUserResponseDto(
                        result.userId(),
                        result.status().name(),
                        result.contractId()
                )
        );
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RejectUserResponseDto> rejectUser(
            @PathVariable UUID id,
            @Valid @RequestBody RejectUserRequestDto request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UUID managerId = principal != null ? principal.userId() : UUID.fromString("e0000000-0000-0000-0000-000000000001");

        RejectUserCommand command = new RejectUserCommand(id, managerId, request.reason());
        RejectUserResultDto result = managerUserUseCase.rejectUser(command);

        return ResponseEntity.ok(
                new RejectUserResponseDto(
                        result.userId(),
                        result.status().name()
                )
        );
    }
}
