package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardAppointmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataYardAppointmentRepository extends JpaRepository<YardAppointmentJpaEntity, UUID> {

    Optional<YardAppointmentJpaEntity> findByInspectionId(UUID inspectionId);

    boolean existsByInspectionId(UUID inspectionId);
}
