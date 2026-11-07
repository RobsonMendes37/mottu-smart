package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.dto.RecentInspectionDto;
import com.mottu.ops.application.port.out.InspectionRepositoryPort;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class InspectionRepositoryAdapter implements InspectionRepositoryPort {

    private static final Set<InspectionStatus> OPEN_STATUSES = Set.of(
            InspectionStatus.QUEUED_AI,
            InspectionStatus.AWAITING_AI_RECOVERY,
            InspectionStatus.HUMAN_TRIAGE
    );

    private final SpringDataInspectionRepository inspectionRepository;

    public InspectionRepositoryAdapter(SpringDataInspectionRepository inspectionRepository) {
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    public int countOpenByContractId(UUID contractId) {
        if (contractId == null) {
            return 0;
        }
        return inspectionRepository.countByContractIdAndStatusIn(contractId, OPEN_STATUSES);
    }

    @Override
    public List<RecentInspectionDto> findRecentByContractId(UUID contractId, int limit) {
        if (contractId == null) {
            return List.of();
        }
        return inspectionRepository.findTop5ByContractIdOrderByCreatedAtDesc(contractId).stream()
                .map(entity -> new RecentInspectionDto(
                        entity.getId(),
                        entity.getStatus(),
                        entity.getFinalVerdict(),
                        entity.getTotalEstimatedBrl(),
                        entity.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public int countOpenByRiderId(UUID riderId) {
        if (riderId == null) {
            return 0;
        }
        return inspectionRepository.countByRiderIdAndStatusIn(riderId, OPEN_STATUSES);
    }

    @Override
    public List<RecentInspectionDto> findRecentByRiderId(UUID riderId, int limit) {
        if (riderId == null) {
            return List.of();
        }
        return inspectionRepository.findTop5ByRiderIdOrderByCreatedAtDesc(riderId).stream()
                .map(entity -> new RecentInspectionDto(
                        entity.getId(),
                        entity.getStatus(),
                        entity.getFinalVerdict(),
                        entity.getTotalEstimatedBrl(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
