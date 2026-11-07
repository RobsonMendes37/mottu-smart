package com.mottu.ops.application.port.out;

import com.mottu.ops.domain.model.RentalContract;

import java.util.Optional;
import java.util.UUID;

public interface RentalContractRepositoryPort {
    RentalContract save(RentalContract contract);
    Optional<RentalContract> findActiveByRiderId(UUID riderId);
    Optional<RentalContract> findActiveByMotorcycleId(UUID motorcycleId);
    Optional<RentalContract> findById(UUID id);
}
