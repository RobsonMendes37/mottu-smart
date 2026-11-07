package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AccidentReportPhotoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAccidentReportPhotoRepository extends JpaRepository<AccidentReportPhotoJpaEntity, UUID> {
    List<AccidentReportPhotoJpaEntity> findByAccidentReportId(UUID accidentReportId);
}

