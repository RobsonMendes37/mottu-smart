package com.mottu.ops.infrastructure.adapter.out.persistence.mapper;

import com.mottu.ops.domain.model.Yard;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardJpaEntity;

public final class YardPersistenceMapper {

    private YardPersistenceMapper() {
    }

    public static Yard toDomain(YardJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Yard(
                entity.getId(),
                entity.getName(),
                entity.getCity(),
                entity.getAddress(),
                entity.isActive()
        );
    }

    public static YardJpaEntity toJpaEntity(Yard domain) {
        if (domain == null) {
            return null;
        }
        return new YardJpaEntity(
                domain.getId(),
                domain.getName(),
                domain.getCity(),
                domain.getAddress(),
                domain.isActive()
        );
    }
}
