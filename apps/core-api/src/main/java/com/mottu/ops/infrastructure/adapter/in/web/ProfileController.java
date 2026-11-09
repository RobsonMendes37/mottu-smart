package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.service.ProfileApplicationService;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdatePasswordRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdatePasswordResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdateProfileRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UserProfileResponseDto;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileApplicationService profileApplicationService;
    private final SpringDataUserRepository userRepository;

    public ProfileController(
            ProfileApplicationService profileApplicationService,
            SpringDataUserRepository userRepository
    ) {
        this.profileApplicationService = profileApplicationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(profileApplicationService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponseDto> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequestDto request,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(profileApplicationService.updateProfile(userId, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<UpdatePasswordResponseDto> updateMyPassword(
            @Valid @RequestBody UpdatePasswordRequestDto request,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(profileApplicationService.updatePassword(userId, request));
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
