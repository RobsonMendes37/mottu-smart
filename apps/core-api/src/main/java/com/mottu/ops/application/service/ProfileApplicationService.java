package com.mottu.ops.application.service;

import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.domain.service.PasswordSecurityPolicy;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdatePasswordRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdatePasswordResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdateProfileRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UserProfileResponseDto;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileApplicationService {

    private final SpringDataUserRepository userRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordSecurityPolicy passwordSecurityPolicy = new PasswordSecurityPolicy();

    public ProfileApplicationService(
            SpringDataUserRepository userRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile(UUID userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));
        return toDto(user);
    }

    @Transactional
    public UserProfileResponseDto updateProfile(UUID userId, UpdateProfileRequestDto request) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email().trim().toLowerCase());
        }
        if (request.pixKey() != null && !request.pixKey().isBlank()) {
            user.setPixKey(request.pixKey().trim());
        }
        if (request.preferredUnit() != null && !request.preferredUnit().isBlank()) {
            user.setPreferredUnit(request.preferredUnit().trim());
        }
        if (request.roleTitle() != null && !request.roleTitle().isBlank()) {
            user.setRoleTitle(request.roleTitle().trim());
        }
        user.setUpdatedAt(Instant.now());

        UserJpaEntity saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional
    public UpdatePasswordResponseDto updatePassword(UUID userId, UpdatePasswordRequestDto request) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Senha atual incorreta.");
        }

        passwordSecurityPolicy.validatePasswordChange(
                request.currentPassword(),
                request.newPassword(),
                user.getPasswordSha256()
        );

        String newSha256 = passwordSecurityPolicy.computeSha256Hex(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordSha256(newSha256);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String fingerprint = newSha256.substring(0, Math.min(8, newSha256.length())) + "...";
        return new UpdatePasswordResponseDto(
                true,
                "Senha atualizada com criptografia SHA-256 + BCrypt em todos os dispositivos.",
                fingerprint
        );
    }

    private UserProfileResponseDto toDto(UserJpaEntity user) {
        String activePlate = null;
        String contractCode = null;

        Optional<RentalContractJpaEntity> contractOpt = rentalContractRepository.findByRiderIdAndActiveTrue(user.getId());
        if (contractOpt.isPresent()) {
            RentalContractJpaEntity contract = contractOpt.get();
            contractCode = contract.getContractCode() != null
                    ? contract.getContractCode()
                    : "CTR-2026-" + contract.getId().toString().replace("-", "").substring(0, 4);

            activePlate = motorcycleRepository.findById(contract.getMotorcycleId())
                    .map(MotorcycleJpaEntity::getPlate)
                    .orElse(null);
        }

        String rawCpf = user.getCpf();
        String maskedCpf = rawCpf != null && rawCpf.length() >= 5
                ? rawCpf.substring(0, 3) + ".***.***-" + rawCpf.substring(rawCpf.length() - 2)
                : rawCpf;

        String sha256 = user.getPasswordSha256();
        String fingerprint = sha256 != null && !sha256.isBlank()
                ? sha256.substring(0, Math.min(8, sha256.length())) + "..."
                : "e4948ce6...";

        String empCode = user.getEmployeeCode() != null
                ? user.getEmployeeCode()
                : "MOT-" + user.getId().toString().replace("-", "").substring(0, 4);

        return new UserProfileResponseDto(
                user.getId(),
                user.getRole(),
                user.getFullName(),
                maskedCpf,
                user.getPhone(),
                user.getEmail(),
                user.getPreferredUnit() != null ? user.getPreferredUnit() : "Unidade Butantã (SP-04)",
                user.getPixKey() != null ? user.getPixKey() : user.getEmail(),
                user.getCnhStatus() != null ? user.getCnhStatus() : "Categoria A • Válida até 08/2029",
                empCode,
                user.getRoleTitle() != null ? user.getRoleTitle() : (user.getRole().name().equals("MANAGER") ? "Gestor da unidade" : "Entregador parceiro"),
                user.getShiftLabel() != null ? user.getShiftLabel() : "Plano Minha Mottu Semanal",
                activePlate,
                contractCode,
                fingerprint,
                user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt()
        );
    }
}

