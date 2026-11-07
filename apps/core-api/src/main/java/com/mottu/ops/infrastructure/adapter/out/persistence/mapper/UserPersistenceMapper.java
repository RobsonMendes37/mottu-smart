package com.mottu.ops.infrastructure.adapter.out.persistence.mapper;

import com.mottu.ops.domain.model.User;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(
                entity.getId(),
                entity.getFullName(),
                entity.getCpf(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getApprovedBy(),
                entity.getCreatedAt()
        );
    }

    public static UserJpaEntity toJpaEntity(User domain) {
        if (domain == null) {
            return null;
        }
        return new UserJpaEntity(
                domain.getId(),
                domain.getFullName(),
                domain.getCpf(),
                domain.getPhone(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getRole(),
                domain.getStatus(),
                domain.getRejectionReason(),
                domain.getApprovedBy(),
                domain.getCreatedAt()
        );
    }
}
