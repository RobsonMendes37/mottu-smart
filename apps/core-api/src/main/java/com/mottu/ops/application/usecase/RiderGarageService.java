package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.ActiveContractDto;
import com.mottu.ops.application.dto.RecentInspectionDto;
import com.mottu.ops.application.dto.RentedMotorcycleDto;
import com.mottu.ops.application.dto.RiderGarageResponse;
import com.mottu.ops.application.port.in.GetRiderGarageUseCase;
import com.mottu.ops.application.port.out.InspectionRepositoryPort;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.domain.model.Yard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RiderGarageService implements GetRiderGarageUseCase {

    private final RentalContractRepositoryPort rentalContractRepositoryPort;
    private final MotorcycleRepositoryPort motorcycleRepositoryPort;
    private final YardRepositoryPort yardRepositoryPort;
    private final InspectionRepositoryPort inspectionRepositoryPort;

    public RiderGarageService(
            RentalContractRepositoryPort rentalContractRepositoryPort,
            MotorcycleRepositoryPort motorcycleRepositoryPort,
            YardRepositoryPort yardRepositoryPort,
            InspectionRepositoryPort inspectionRepositoryPort
    ) {
        this.rentalContractRepositoryPort = rentalContractRepositoryPort;
        this.motorcycleRepositoryPort = motorcycleRepositoryPort;
        this.yardRepositoryPort = yardRepositoryPort;
        this.inspectionRepositoryPort = inspectionRepositoryPort;
    }

    @Override
    public RiderGarageResponse getGarageForRider(UUID riderId) {
        if (riderId == null) {
            return new RiderGarageResponse(null, 0, List.of());
        }

        Optional<RentalContract> activeContractOpt = rentalContractRepositoryPort.findActiveByRiderId(riderId);
        if (activeContractOpt.isEmpty()) {
            return new RiderGarageResponse(null, 0, List.of());
        }

        RentalContract contract = activeContractOpt.get();
        Motorcycle motorcycle = motorcycleRepositoryPort.findById(contract.getMotorcycleId())
                .orElse(null);

        String yardName = "Galpão Central";
        if (motorcycle != null) {
            yardName = yardRepositoryPort.findById(motorcycle.getYardId())
                    .map(Yard::getName)
                    .orElse("Galpão Central");
        }

        RentedMotorcycleDto motoDto = null;
        if (motorcycle != null) {
            motoDto = new RentedMotorcycleDto(
                    motorcycle.getId(),
                    motorcycle.getPlate(),
                    motorcycle.getModel(),
                    motorcycle.getStatus(),
                    motorcycle.getCurrentOdometerKm(),
                    yardName
            );
        }

        int currentOdo = motorcycle != null ? motorcycle.getCurrentOdometerKm() : contract.getInitialOdometerKm();
        com.mottu.ops.domain.model.BaselineShieldStatus shield = com.mottu.ops.domain.model.BaselineShieldStatus.calculate(
                contract.getPickedUpAt(),
                contract.getInitialOdometerKm(),
                currentOdo,
                java.time.Instant.now()
        );

        ActiveContractDto contractDto = new ActiveContractDto(
                contract.getId(),
                contract.getPickedUpAt(),
                contract.getInitialOdometerKm(),
                motoDto,
                contract.getPickupPhotosJson(),
                contract.isPickupValidatedByRider(),
                contract.getPickupValidatedAt(),
                shield.isShieldActive(),
                shield.remainingHours(),
                shield.remainingKm()
        );

        int openCount = inspectionRepositoryPort.countOpenByContractId(contract.getId());
        List<RecentInspectionDto> recent = inspectionRepositoryPort.findRecentByContractId(contract.getId(), 5);

        return new RiderGarageResponse(contractDto, openCount, recent);
    }

    @Override
    public ActiveContractDto validatePickup(UUID riderId) {
        if (riderId == null) {
            throw new IllegalArgumentException("Rider ID não pode ser nulo");
        }
        RentalContract contract = rentalContractRepositoryPort.findActiveByRiderId(riderId)
                .orElseThrow(() -> new com.mottu.ops.domain.exception.BusinessRuleViolationException("Nenhum contrato ativo encontrado para este entregador"));
        contract.validatePickup(java.time.Instant.now());
        RentalContract saved = rentalContractRepositoryPort.save(contract);

        Motorcycle motorcycle = motorcycleRepositoryPort.findById(saved.getMotorcycleId()).orElse(null);
        String yardName = "Galpão Central";
        if (motorcycle != null) {
            yardName = yardRepositoryPort.findById(motorcycle.getYardId())
                    .map(Yard::getName)
                    .orElse("Galpão Central");
        }

        RentedMotorcycleDto motoDto = null;
        if (motorcycle != null) {
            motoDto = new RentedMotorcycleDto(
                    motorcycle.getId(),
                    motorcycle.getPlate(),
                    motorcycle.getModel(),
                    motorcycle.getStatus(),
                    motorcycle.getCurrentOdometerKm(),
                    yardName
            );
        }

        int currentOdo = motorcycle != null ? motorcycle.getCurrentOdometerKm() : saved.getInitialOdometerKm();
        com.mottu.ops.domain.model.BaselineShieldStatus shield = com.mottu.ops.domain.model.BaselineShieldStatus.calculate(
                saved.getPickedUpAt(),
                saved.getInitialOdometerKm(),
                currentOdo,
                java.time.Instant.now()
        );

        return new ActiveContractDto(
                saved.getId(),
                saved.getPickedUpAt(),
                saved.getInitialOdometerKm(),
                motoDto,
                saved.getPickupPhotosJson(),
                saved.isPickupValidatedByRider(),
                saved.getPickupValidatedAt(),
                shield.isShieldActive(),
                shield.remainingHours(),
                shield.remainingKm()
        );
    }
}
