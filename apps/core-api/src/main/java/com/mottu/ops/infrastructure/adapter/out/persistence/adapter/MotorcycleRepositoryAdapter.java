package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.dto.MotorcycleFilter;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.mapper.MotorcyclePersistenceMapper;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MotorcycleRepositoryAdapter implements MotorcycleRepositoryPort {

    private final SpringDataMotorcycleRepository springDataMotorcycleRepository;

    public MotorcycleRepositoryAdapter(SpringDataMotorcycleRepository springDataMotorcycleRepository) {
        this.springDataMotorcycleRepository = springDataMotorcycleRepository;
    }

    @Override
    public Optional<Motorcycle> findById(UUID id) {
        return springDataMotorcycleRepository.findById(id)
                .map(MotorcyclePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Motorcycle> findByPlate(String plate) {
        if (plate == null) {
            return Optional.empty();
        }
        return springDataMotorcycleRepository.findByPlateIgnoreCase(plate.trim())
                .map(MotorcyclePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByPlate(String plate) {
        if (plate == null) {
            return false;
        }
        return springDataMotorcycleRepository.existsByPlateIgnoreCase(plate.trim());
    }

    @Override
    public Motorcycle save(Motorcycle motorcycle) {
        MotorcycleJpaEntity entity = MotorcyclePersistenceMapper.toJpaEntity(motorcycle);
        MotorcycleJpaEntity saved = springDataMotorcycleRepository.save(entity);
        return MotorcyclePersistenceMapper.toDomain(saved);
    }

    @Override
    public List<Motorcycle> findAll(MotorcycleFilter filter, int page, int size) {
        Specification<MotorcycleJpaEntity> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return springDataMotorcycleRepository.findAll(spec, pageable).getContent().stream()
                .map(MotorcyclePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long count(MotorcycleFilter filter) {
        Specification<MotorcycleJpaEntity> spec = buildSpecification(filter);
        return springDataMotorcycleRepository.count(spec);
    }

    private Specification<MotorcycleJpaEntity> buildSpecification(MotorcycleFilter filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.plate() != null && !filter.plate().isBlank()) {
                String pattern = "%" + filter.plate().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("plate")), pattern));
            }
            if (filter.yardId() != null) {
                predicates.add(cb.equal(root.get("yardId"), filter.yardId()));
            }
            if (filter.model() != null) {
                predicates.add(cb.equal(root.get("model"), filter.model()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
