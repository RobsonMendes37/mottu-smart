package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.LoginCommand;
import com.mottu.ops.application.dto.LoginResultDto;
import com.mottu.ops.application.dto.PublicConfigDto;
import com.mottu.ops.application.dto.RegisterCommand;
import com.mottu.ops.application.dto.RegisterResultDto;
import com.mottu.ops.application.dto.UserProfileDto;
import com.mottu.ops.application.exception.AccountPendingApprovalException;
import com.mottu.ops.application.exception.AccountRejectedException;
import com.mottu.ops.application.exception.DuplicateResourceException;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.AuthUseCase;
import com.mottu.ops.application.port.out.PasswordEncoderPort;
import com.mottu.ops.application.port.out.TokenProviderPort;
import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final boolean publicDemoMode;

    public AuthService(
            UserRepositoryPort userRepository,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider,
            boolean publicDemoMode
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.publicDemoMode = publicDemoMode;
    }

    @Override
    public PublicConfigDto getPublicConfig() {
        return new PublicConfigDto(this.publicDemoMode);
    }

    @Override
    public RegisterResultDto register(RegisterCommand command) {
        if (command.role() == UserRole.MANAGER) {
            throw new DomainValidationException("MANAGER role self-registration is not allowed. Only RIDER and MECHANIC can register.");
        }
        if (userRepository.existsByCpf(command.cpf())) {
            throw new DuplicateResourceException("CPF already registered: " + command.cpf());
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateResourceException("Email already registered: " + command.email());
        }

        String encodedPassword = passwordEncoder.encode(command.password());

        User newUser = new User(
                UUID.randomUUID(),
                command.fullName(),
                command.cpf(),
                command.phone(),
                command.email(),
                encodedPassword,
                command.role(),
                AccountStatus.PENDING_APPROVAL,
                null,
                null,
                Instant.now()
        );

        User savedUser = userRepository.save(newUser);

        return new RegisterResultDto(
                savedUser.getId(),
                savedUser.getStatus(),
                "Cadastro recebido! Aguarde a liberação do Gestor."
        );
    }

    @Override
    public LoginResultDto login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        if (user.getStatus() == AccountStatus.PENDING_APPROVAL) {
            throw new AccountPendingApprovalException("Conta aguardando aprovação do Gestor.");
        }

        if (user.getStatus() == AccountStatus.REJECTED) {
            String reason = user.getRejectionReason() != null ? user.getRejectionReason() : "Sem motivo informado";
            throw new AccountRejectedException("Cadastro rejeitado: " + reason);
        }

        if (!user.isActive()) {
            throw new AccountPendingApprovalException("Conta não está ativa.");
        }

        String token = tokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        );

        return new LoginResultDto(
                token,
                user.getId(),
                user.getRole(),
                user.getFullName()
        );
    }

    @Override
    public UserProfileDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCpf(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}
