package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AccidentReportJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAccidentReportRepository extends JpaRepository<AccidentReportJpaEntity, UUID> {
    Optional<AccidentReportJpaEntity> findByIdempotencyKey(String idempotencyKey);
    Page<AccidentReportJpaEntity> findByStatus(AccidentReportStatus status, Pageable pageable);
    Page<AccidentReportJpaEntity> findByRiderId(UUID riderId, Pageable pageable);
}

