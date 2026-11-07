package com.mottu.ops.application.port.out;

import com.mottu.ops.domain.model.Yard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YardRepositoryPort {
    List<Yard> findAllActive();
    Optional<Yard> findById(UUID id);
    Yard save(Yard yard);
}
