package com.mottu.ops.application.port.out;

import com.mottu.ops.application.dto.MotorcycleFilter;
import com.mottu.ops.domain.model.Motorcycle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MotorcycleRepositoryPort {
    Optional<Motorcycle> findById(UUID id);
    Optional<Motorcycle> findByPlate(String plate);
    boolean existsByPlate(String plate);
    Motorcycle save(Motorcycle motorcycle);
    List<Motorcycle> findAll(MotorcycleFilter filter, int page, int size);
    long count(MotorcycleFilter filter);
}
