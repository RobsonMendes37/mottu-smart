package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMotorcycleRepository extends JpaRepository<MotorcycleJpaEntity, UUID>, JpaSpecificationExecutor<MotorcycleJpaEntity> {
    Optional<MotorcycleJpaEntity> findByPlate(String plate);
    Optional<MotorcycleJpaEntity> findByPlateIgnoreCase(String plate);
    boolean existsByPlateIgnoreCase(String plate);
    List<MotorcycleJpaEntity> findByStatus(MotorcycleStatus status);
    long countByStatus(MotorcycleStatus status);
}

