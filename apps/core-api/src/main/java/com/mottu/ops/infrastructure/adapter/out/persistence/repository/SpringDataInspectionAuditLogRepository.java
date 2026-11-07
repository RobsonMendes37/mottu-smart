package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionAuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataInspectionAuditLogRepository extends JpaRepository<InspectionAuditLogJpaEntity, UUID> {
    List<InspectionAuditLogJpaEntity> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);
}
