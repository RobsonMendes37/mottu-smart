package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.mapper.RentalContractPersistenceMapper;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RentalContractRepositoryAdapter implements RentalContractRepositoryPort {

    private final SpringDataRentalContractRepository springDataRentalContractRepository;

    public RentalContractRepositoryAdapter(SpringDataRentalContractRepository springDataRentalContractRepository) {
        this.springDataRentalContractRepository = springDataRentalContractRepository;
    }

    @Override
    public RentalContract save(RentalContract contract) {
        RentalContractJpaEntity entity = RentalContractPersistenceMapper.toJpaEntity(contract);
        RentalContractJpaEntity saved = springDataRentalContractRepository.save(entity);
        return RentalContractPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<RentalContract> findActiveByRiderId(UUID riderId) {
        return springDataRentalContractRepository.findByRiderIdAndActiveTrue(riderId)
                .map(RentalContractPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RentalContract> findActiveByMotorcycleId(UUID motorcycleId) {
        return springDataRentalContractRepository.findByMotorcycleIdAndActiveTrue(motorcycleId)
                .map(RentalContractPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RentalContract> findById(UUID id) {
        return springDataRentalContractRepository.findById(id)
                .map(RentalContractPersistenceMapper::toDomain);
    }
}
