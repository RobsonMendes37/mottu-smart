package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.ServiceOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataServiceOrderRepository extends JpaRepository<ServiceOrderJpaEntity, UUID> {

    Optional<ServiceOrderJpaEntity> findByCode(String code);

    boolean existsByInspectionId(UUID inspectionId);

    List<ServiceOrderJpaEntity> findAllByOrderByCreatedAtDesc();
}

