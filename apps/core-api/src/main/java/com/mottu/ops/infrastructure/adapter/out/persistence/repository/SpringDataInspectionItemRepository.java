package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataInspectionItemRepository extends JpaRepository<InspectionItemJpaEntity, UUID> {
    List<InspectionItemJpaEntity> findByInspectionId(UUID inspectionId);
    void deleteByInspectionId(UUID inspectionId);
}
