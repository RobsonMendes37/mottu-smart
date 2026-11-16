package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.dto.RecentInspectionDto;
import com.mottu.ops.application.port.out.InspectionRepositoryPort;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionItemJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionItemRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final SpringDataInspectionItemRepository inspectionItemRepository;

    public InspectionRepositoryAdapter(SpringDataInspectionRepository inspectionRepository) {
        this(inspectionRepository, null);
    }

    @Autowired
    public InspectionRepositoryAdapter(
            SpringDataInspectionRepository inspectionRepository,
            @Autowired(required = false) SpringDataInspectionItemRepository inspectionItemRepository
    ) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
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
                .map(this::toRecentDto)
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
                .map(this::toRecentDto)
                .toList();
    }

    private RecentInspectionDto toRecentDto(InspectionJpaEntity entity) {
        List<String> partNames = List.of();
        if (inspectionItemRepository != null && entity.getId() != null) {
            try {
                partNames = inspectionItemRepository.findByInspectionId(entity.getId()).stream()
                        .map(InspectionItemJpaEntity::getPartNameSnapshot)
                        .filter(name -> name != null && !name.isBlank())
                        .toList();
            } catch (Exception ignored) {
                partNames = List.of();
            }
        }
        return new RecentInspectionDto(
                entity.getId(),
                entity.getStatus(),
                entity.getFinalVerdict(),
                entity.getTotalEstimatedBrl(),
                entity.getCreatedAt(),
                partNames.size(),
                partNames
        );
    }
}
