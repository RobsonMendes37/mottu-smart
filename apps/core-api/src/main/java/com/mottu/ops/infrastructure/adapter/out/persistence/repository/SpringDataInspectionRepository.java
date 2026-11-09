package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInspectionRepository extends JpaRepository<InspectionJpaEntity, UUID> {

    int countByContractIdAndStatusIn(UUID contractId, Collection<InspectionStatus> statuses);

    List<InspectionJpaEntity> findByContractId(UUID contractId);

    List<InspectionJpaEntity> findByRiderId(UUID riderId);

    List<InspectionJpaEntity> findTop5ByContractIdOrderByCreatedAtDesc(UUID contractId);

    int countByRiderIdAndStatusIn(UUID riderId, Collection<InspectionStatus> statuses);

    List<InspectionJpaEntity> findTop5ByRiderIdOrderByCreatedAtDesc(UUID riderId);

    Optional<InspectionJpaEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByPhotoSha256(String photoSha256);

    @Query("SELECT i.photoPerceptualHash FROM InspectionJpaEntity i WHERE i.photoPerceptualHash IS NOT NULL")
    List<String> findAllPhotoPerceptualHashes();

    List<InspectionJpaEntity> findByStatus(InspectionStatus status);
    long countByStatus(InspectionStatus status);

    @Query("SELECT i FROM InspectionJpaEntity i WHERE i.status = :status " +
            "AND (:reason IS NULL OR i.priorityReason = :reason) " +
            "ORDER BY i.priorityWeight ASC, i.createdAt ASC")
    Page<InspectionJpaEntity> findMechanicQueue(
            @Param("status") InspectionStatus status,
            @Param("reason") QueuePriorityReason reason,
            Pageable pageable
    );
}

