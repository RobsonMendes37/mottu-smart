package com.mottu.ops.infrastructure.adapter.out.persistence.mapper;

import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;

public final class MotorcyclePersistenceMapper {

    private MotorcyclePersistenceMapper() {
    }

    public static Motorcycle toDomain(MotorcycleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Motorcycle(
                entity.getId(),
                entity.getPlate(),
                entity.getModel(),
                entity.getYardId(),
                entity.getStatus(),
                entity.getCurrentOdometerKm(),
                entity.getCreatedAt()
        );
    }

    public static MotorcycleJpaEntity toJpaEntity(Motorcycle domain) {
        if (domain == null) {
            return null;
        }
        return new MotorcycleJpaEntity(
                domain.getId(),
                domain.getPlate(),
                domain.getModel(),
                domain.getYardId(),
                domain.getStatus(),
                domain.getCurrentOdometerKm(),
                domain.getCreatedAt()
        );
    }
}
