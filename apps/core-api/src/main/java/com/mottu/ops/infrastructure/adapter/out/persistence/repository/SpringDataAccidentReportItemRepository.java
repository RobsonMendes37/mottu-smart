package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AccidentReportItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAccidentReportItemRepository extends JpaRepository<AccidentReportItemJpaEntity, UUID> {
    List<AccidentReportItemJpaEntity> findByAccidentReportId(UUID accidentReportId);
    void deleteByAccidentReportId(UUID accidentReportId);
}

