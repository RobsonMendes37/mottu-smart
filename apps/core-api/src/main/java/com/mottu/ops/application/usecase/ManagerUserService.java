package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.ApproveUserCommand;
import com.mottu.ops.application.dto.ApproveUserResultDto;
import com.mottu.ops.application.dto.PendingUserDto;
import com.mottu.ops.application.dto.RejectUserCommand;
import com.mottu.ops.application.dto.RejectUserResultDto;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.ManagerUserUseCase;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.domain.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ManagerUserService implements ManagerUserUseCase {

    private final UserRepositoryPort userRepository;
    private final MotorcycleRepositoryPort motorcycleRepository;
    private final RentalContractRepositoryPort rentalContractRepository;

    public ManagerUserService(
            UserRepositoryPort userRepository,
            MotorcycleRepositoryPort motorcycleRepository,
            RentalContractRepositoryPort rentalContractRepository
    ) {
        this.userRepository = userRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.rentalContractRepository = rentalContractRepository;
    }

    @Override
    public List<PendingUserDto> getPendingUsers() {
        return userRepository.findByStatus(AccountStatus.PENDING_APPROVAL)
                .stream()
                .map(u -> new PendingUserDto(
                        u.getId(),
                        u.getFullName(),
                        u.getCpf(),
                        u.getPhone(),
                        u.getEmail(),
                        u.getRole(),
                        u.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public ApproveUserResultDto approveUser(ApproveUserCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + command.userId()));

        UUID contractId = null;

        if (user.getRole() == UserRole.RIDER) {
            if (command.motorcycleId() == null) {
                throw new DomainValidationException("motorcycleId is required when approving a RIDER user");
            }

            Motorcycle motorcycle = motorcycleRepository.findById(command.motorcycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Motorcycle not found with id: " + command.motorcycleId()));

            motorcycle.rent();
            motorcycleRepository.save(motorcycle);

            RentalContract contract = RentalContract.create(
                    user.getId(),
                    motorcycle.getId(),
                    Instant.now(),
                    motorcycle.getCurrentOdometerKm()
            );
            RentalContract savedContract = rentalContractRepository.save(contract);
            contractId = savedContract.getId();
        }

        user.approve(command.managerId());
        User savedUser = userRepository.save(user);

        return new ApproveUserResultDto(
                savedUser.getId(),
                savedUser.getStatus(),
                contractId
        );
    }

    @Override
    public RejectUserResultDto rejectUser(RejectUserCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw new DomainValidationException("Rejection reason cannot be blank");
        }

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + command.userId()));

        user.reject(command.managerId(), command.reason().trim());
        User savedUser = userRepository.save(user);

        return new RejectUserResultDto(
                savedUser.getId(),
                savedUser.getStatus()
        );
    }
}
