package com.mottu.ops.infrastructure.adapter.out.persistence.mapper;

import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;

public final class RentalContractPersistenceMapper {

    private RentalContractPersistenceMapper() {
    }

    public static RentalContract toDomain(RentalContractJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RentalContract(
                entity.getId(),
                entity.getRiderId(),
                entity.getMotorcycleId(),
                entity.getPickedUpAt(),
                entity.getInitialOdometerKm(),
                entity.getEndedAt(),
                entity.isActive(),
                entity.getPickupPhotosJson(),
                entity.isPickupValidatedByRider(),
                entity.getPickupValidatedAt()
        );
    }

    public static RentalContractJpaEntity toJpaEntity(RentalContract domain) {
        if (domain == null) {
            return null;
        }
        RentalContractJpaEntity entity = new RentalContractJpaEntity(
                domain.getId(),
                domain.getRiderId(),
                domain.getMotorcycleId(),
                domain.getPickedUpAt(),
                domain.getInitialOdometerKm(),
                domain.getEndedAt(),
                domain.isActive()
        );
        entity.setPickupPhotosJson(domain.getPickupPhotosJson());
        entity.setPickupValidatedByRider(domain.isPickupValidatedByRider());
        entity.setPickupValidatedAt(domain.getPickupValidatedAt());
        return entity;
    }
}
