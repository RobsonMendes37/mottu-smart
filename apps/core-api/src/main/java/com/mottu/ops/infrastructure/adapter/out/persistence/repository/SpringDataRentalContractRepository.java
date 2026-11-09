package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRentalContractRepository extends JpaRepository<RentalContractJpaEntity, UUID> {
    Optional<RentalContractJpaEntity> findByRiderIdAndActiveTrue(UUID riderId);
    Optional<RentalContractJpaEntity> findByMotorcycleIdAndActiveTrue(UUID motorcycleId);
    Optional<RentalContractJpaEntity> findByContractCode(String contractCode);
    java.util.List<RentalContractJpaEntity> findAllByActiveTrue();
}
