package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.CreateMotorcycleCommand;
import com.mottu.ops.application.dto.FleetMotorcycleResponse;
import com.mottu.ops.application.dto.MotorcycleDetailsResponse;
import com.mottu.ops.application.dto.MotorcycleFilter;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.RecordBaseline360Command;
import com.mottu.ops.application.exception.DuplicateResourceException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.FleetManagementUseCase;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.model.Baseline360Photos;
import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.domain.model.User;
import com.mottu.ops.domain.model.Yard;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FleetService implements FleetManagementUseCase {

    private final MotorcycleRepositoryPort motorcycleRepositoryPort;
    private final YardRepositoryPort yardRepositoryPort;
    private final RentalContractRepositoryPort rentalContractRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ObjectMapper objectMapper;

    public FleetService(
            MotorcycleRepositoryPort motorcycleRepositoryPort,
            YardRepositoryPort yardRepositoryPort,
            RentalContractRepositoryPort rentalContractRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            ObjectMapper objectMapper
    ) {
        this.motorcycleRepositoryPort = motorcycleRepositoryPort;
        this.yardRepositoryPort = yardRepositoryPort;
        this.rentalContractRepositoryPort = rentalContractRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public PageResponse<FleetMotorcycleResponse> listMotorcycles(MotorcycleFilter filter, int page, int size) {
        List<Motorcycle> motorcycles = motorcycleRepositoryPort.findAll(filter, page, size);
        long total = motorcycleRepositoryPort.count(filter);

        List<FleetMotorcycleResponse> items = motorcycles.stream()
                .map(this::toFleetResponse)
                .toList();

        return PageResponse.of(items, page, size, total);
    }

    @Override
    public MotorcycleDetailsResponse createMotorcycle(CreateMotorcycleCommand command) {
        Yard yard = yardRepositoryPort.findById(command.yardId())
                .orElseThrow(() -> new ResourceNotFoundException("Galpão não encontrado com id: " + command.yardId()));

        if (motorcycleRepositoryPort.existsByPlate(command.plate())) {
            throw new DuplicateResourceException("Já existe uma motocicleta cadastrada com a placa: " + command.plate());
        }

        Motorcycle motorcycle = new Motorcycle(
                UUID.randomUUID(),
                command.plate(),
                command.model(),
                command.yardId(),
                MotorcycleStatus.AVAILABLE,
                command.currentOdometerKm(),
                Instant.now()
        );

        Motorcycle saved = motorcycleRepositoryPort.save(motorcycle);
        return toDetailsResponse(saved, yard.getName(), null);
    }

    @Override
    public MotorcycleDetailsResponse getMotorcycleById(UUID id) {
        Motorcycle motorcycle = motorcycleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motocicleta não encontrada com id: " + id));

        String yardName = yardRepositoryPort.findById(motorcycle.getYardId())
                .map(Yard::getName)
                .orElse("Galpão Central");

        String activeRiderName = resolveActiveRiderName(motorcycle);
        return toDetailsResponse(motorcycle, yardName, activeRiderName);
    }

    @Override
    public MotorcycleDetailsResponse updateMotorcycleStatus(UUID id, MotorcycleStatus newStatus) {
        Motorcycle motorcycle = motorcycleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motocicleta não encontrada com id: " + id));

        if (motorcycle.getStatus() == MotorcycleStatus.RENTED && newStatus != MotorcycleStatus.RENTED) {
            throw new BusinessRuleViolationException(
                    "Motocicleta alugada não pode ter o status alterado diretamente para " + newStatus + ". Encerre o contrato ativo primeiro."
            );
        }

        if (newStatus == MotorcycleStatus.IN_MAINTENANCE) {
            motorcycle.sendToMaintenance();
        } else if (newStatus == MotorcycleStatus.AVAILABLE) {
            motorcycle.releaseFromMaintenance();
        } else if (newStatus == MotorcycleStatus.RENTED) {
            motorcycle.rent();
        }

        Motorcycle saved = motorcycleRepositoryPort.save(motorcycle);

        String yardName = yardRepositoryPort.findById(saved.getYardId())
                .map(Yard::getName)
                .orElse("Galpão Central");

        String activeRiderName = resolveActiveRiderName(saved);
        return toDetailsResponse(saved, yardName, activeRiderName);
    }

    private FleetMotorcycleResponse toFleetResponse(Motorcycle moto) {
        String yardName = yardRepositoryPort.findById(moto.getYardId())
                .map(Yard::getName)
                .orElse("Galpão Central");

        String activeRiderName = resolveActiveRiderName(moto);

        return new FleetMotorcycleResponse(
                moto.getId(),
                moto.getPlate(),
                moto.getModel(),
                moto.getYardId(),
                yardName,
                moto.getStatus(),
                moto.getCurrentOdometerKm(),
                activeRiderName
        );
    }

    @Override
    public MotorcycleDetailsResponse recordBaseline360(UUID id, RecordBaseline360Command command) {
        Motorcycle motorcycle = motorcycleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motocicleta não encontrada: " + id));

        Baseline360Photos baseline = new Baseline360Photos(
                command.frontPhotoUrl(),
                command.rearPhotoUrl(),
                command.rightSidePhotoUrl(),
                command.leftSidePhotoUrl(),
                command.odometerPhotoUrl(),
                command.initialOdometerKm(),
                Instant.now()
        );

        String photosJson;
        try {
            photosJson = objectMapper.writeValueAsString(baseline);
        } catch (Exception e) {
            photosJson = "{}";
        }

        Optional<RentalContract> contractOpt = rentalContractRepositoryPort.findActiveByMotorcycleId(id);
        if (contractOpt.isPresent()) {
            RentalContract contract = contractOpt.get();
            contract.updatePickupPhotos(photosJson);
            rentalContractRepositoryPort.save(contract);
        }

        String yardName = yardRepositoryPort.findById(motorcycle.getYardId())
                .map(Yard::getName)
                .orElse("Galpão Central");

        String activeRiderName = resolveActiveRiderName(motorcycle);
        return new MotorcycleDetailsResponse(
                motorcycle.getId(),
                motorcycle.getPlate(),
                motorcycle.getModel(),
                motorcycle.getYardId(),
                yardName,
                motorcycle.getStatus(),
                motorcycle.getCurrentOdometerKm(),
                activeRiderName,
                motorcycle.getCreatedAt(),
                photosJson
        );
    }

    private MotorcycleDetailsResponse toDetailsResponse(Motorcycle moto, String yardName, String activeRiderName) {
        String pickupPhotosJson = rentalContractRepositoryPort.findActiveByMotorcycleId(moto.getId())
                .map(RentalContract::getPickupPhotosJson)
                .orElse(null);

        return new MotorcycleDetailsResponse(
                moto.getId(),
                moto.getPlate(),
                moto.getModel(),
                moto.getYardId(),
                yardName,
                moto.getStatus(),
                moto.getCurrentOdometerKm(),
                activeRiderName,
                moto.getCreatedAt(),
                pickupPhotosJson
        );
    }

    private String resolveActiveRiderName(Motorcycle moto) {
        if (moto.getStatus() != MotorcycleStatus.RENTED) {
            return null;
        }
        Optional<RentalContract> contractOpt = rentalContractRepositoryPort.findActiveByMotorcycleId(moto.getId());
        if (contractOpt.isEmpty()) {
            return null;
        }
        return userRepositoryPort.findById(contractOpt.get().getRiderId())
                .map(User::getFullName)
                .orElse(null);
    }
}
