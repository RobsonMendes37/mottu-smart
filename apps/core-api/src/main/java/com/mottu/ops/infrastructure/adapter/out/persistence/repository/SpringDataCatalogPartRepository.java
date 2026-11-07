package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.CatalogPartJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCatalogPartRepository extends JpaRepository<CatalogPartJpaEntity, UUID> {

    Optional<CatalogPartJpaEntity> findByVtexSkuAndMotorcycleModel(String vtexSku, MotorcycleModel motorcycleModel);

    @Query("SELECT c FROM CatalogPartJpaEntity c WHERE " +
            "(:model IS NULL OR c.motorcycleModel = :model) AND " +
            "(:search IS NULL OR LOWER(c.partName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.vtexSku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CatalogPartJpaEntity> searchParts(
            @Param("model") MotorcycleModel model,
            @Param("search") String search,
            Pageable pageable
    );
}
