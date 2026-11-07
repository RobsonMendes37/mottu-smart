package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.SystemSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataSystemSettingsRepository extends JpaRepository<SystemSettingsJpaEntity, String> {

    Optional<SystemSettingsJpaEntity> findBySettingKey(String settingKey);
}
