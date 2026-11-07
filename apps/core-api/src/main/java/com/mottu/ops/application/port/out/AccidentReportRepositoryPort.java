package com.mottu.ops.application.port.out;

import com.mottu.ops.domain.model.AccidentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AccidentReportRepositoryPort {
    AccidentReport save(AccidentReport report);
    Optional<AccidentReport> findById(UUID id);
    Optional<AccidentReport> findByIdempotencyKey(String idempotencyKey);
    Page<AccidentReport> findPage(Pageable pageable);
}

