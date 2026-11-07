package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.LoginCommand;
import com.mottu.ops.application.dto.LoginResultDto;
import com.mottu.ops.application.dto.PublicConfigDto;
import com.mottu.ops.application.dto.RegisterCommand;
import com.mottu.ops.application.dto.RegisterResultDto;
import com.mottu.ops.application.dto.UserProfileDto;
import com.mottu.ops.application.port.in.AuthUseCase;
import com.mottu.ops.infrastructure.adapter.in.web.dto.LoginRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.LoginResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.PublicConfigResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.RegisterRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.RegisterResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.UserProfileResponseDto;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @GetMapping("/public-config")
    public ResponseEntity<PublicConfigResponseDto> getPublicConfig() {
        PublicConfigDto config = authUseCase.getPublicConfig();
        return ResponseEntity.ok(new PublicConfigResponseDto(config.publicDemoMode()));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        RegisterCommand command = new RegisterCommand(
                request.fullName(),
                request.cpf(),
                request.phone(),
                request.email(),
                request.password(),
                request.role()
        );
        RegisterResultDto result = authUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RegisterResponseDto(
                        result.userId(),
                        result.status().name(),
                        result.message()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        LoginResultDto result = authUseCase.login(command);
        return ResponseEntity.ok(
                new LoginResponseDto(
                        result.accessToken(),
                        result.userId(),
                        result.role().name(),
                        result.fullName()
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDto> getProfile(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UserProfileDto profile = authUseCase.getProfile(principal.userId());
        return ResponseEntity.ok(
                new UserProfileResponseDto(
                        profile.userId(),
                        profile.fullName(),
                        profile.email(),
                        profile.cpf(),
                        profile.phone(),
                        profile.role().name(),
                        profile.status().name()
                )
        );
    }
}
