package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.domain.model.Yard;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.mapper.YardPersistenceMapper;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataYardRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class YardRepositoryAdapter implements YardRepositoryPort {

    private final SpringDataYardRepository yardRepository;

    public YardRepositoryAdapter(SpringDataYardRepository yardRepository) {
        this.yardRepository = yardRepository;
    }

    @Override
    public List<Yard> findAllActive() {
        return yardRepository.findByActiveTrue().stream()
                .map(YardPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Yard> findById(UUID id) {
        return yardRepository.findById(id)
                .map(YardPersistenceMapper::toDomain);
    }

    @Override
    public Yard save(Yard yard) {
        YardJpaEntity entity = YardPersistenceMapper.toJpaEntity(yard);
        YardJpaEntity saved = yardRepository.save(entity);
        return YardPersistenceMapper.toDomain(saved);
    }
}
