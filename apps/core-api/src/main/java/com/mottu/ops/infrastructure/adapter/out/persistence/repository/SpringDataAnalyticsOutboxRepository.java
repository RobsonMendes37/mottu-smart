package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AnalyticsOutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAnalyticsOutboxRepository extends JpaRepository<AnalyticsOutboxJpaEntity, UUID> {
    List<AnalyticsOutboxJpaEntity> findByExportedFalseOrderByCreatedAtAsc();
}
