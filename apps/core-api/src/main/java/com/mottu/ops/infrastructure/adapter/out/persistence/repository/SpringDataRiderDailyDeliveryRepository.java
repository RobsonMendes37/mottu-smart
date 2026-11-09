package com.mottu.ops.infrastructure.adapter.out.persistence.repository;

import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RiderDailyDeliveryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRiderDailyDeliveryRepository extends JpaRepository<RiderDailyDeliveryJpaEntity, UUID> {

    List<RiderDailyDeliveryJpaEntity> findByContractIdOrderByDeliveryDateAsc(UUID contractId);

    List<RiderDailyDeliveryJpaEntity> findByRiderIdOrderByDeliveryDateAsc(UUID riderId);

    Optional<RiderDailyDeliveryJpaEntity> findByRiderIdAndDeliveryDate(UUID riderId, LocalDate deliveryDate);

    Optional<RiderDailyDeliveryJpaEntity> findByContractIdAndDeliveryDate(UUID contractId, LocalDate deliveryDate);
}

