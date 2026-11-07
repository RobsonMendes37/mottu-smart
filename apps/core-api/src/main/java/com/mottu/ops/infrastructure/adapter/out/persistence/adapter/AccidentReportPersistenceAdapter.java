package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.port.out.AccidentReportRepositoryPort;
import com.mottu.ops.domain.model.AccidentBudget;
import com.mottu.ops.domain.model.AccidentReport;
import com.mottu.ops.domain.model.AccidentReportItem;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AccidentReportItemJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AccidentReportJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AccidentReportPhotoJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataAccidentReportItemRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataAccidentReportPhotoRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataAccidentReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccidentReportPersistenceAdapter implements AccidentReportRepositoryPort {

    private final SpringDataAccidentReportRepository accidentReportRepository;
    private final SpringDataAccidentReportItemRepository itemRepository;
    private final SpringDataAccidentReportPhotoRepository photoRepository;

    public AccidentReportPersistenceAdapter(
            SpringDataAccidentReportRepository accidentReportRepository,
            SpringDataAccidentReportItemRepository itemRepository,
            SpringDataAccidentReportPhotoRepository photoRepository
    ) {
        this.accidentReportRepository = accidentReportRepository;
        this.itemRepository = itemRepository;
        this.photoRepository = photoRepository;
    }

    @Override
    @Transactional
    public AccidentReport save(AccidentReport report) {
        AccidentReportJpaEntity entity = toJpaEntity(report);
        accidentReportRepository.save(entity);

        // Sync photos
        List<AccidentReportPhotoJpaEntity> existingPhotos = photoRepository.findByAccidentReportId(report.getId());
        Set<String> existingKeys = existingPhotos.stream()
                .map(AccidentReportPhotoJpaEntity::getPhotoObjectKey)
                .collect(Collectors.toSet());

        for (String photoKey : report.getPhotoObjectKeys()) {
            if (!existingKeys.contains(photoKey)) {
                photoRepository.save(new AccidentReportPhotoJpaEntity(
                        UUID.randomUUID(),
                        report.getId(),
                        photoKey,
                        Instant.now()
                ));
            }
        }

        // Sync items
        itemRepository.deleteByAccidentReportId(report.getId());
        for (AccidentReportItem item : report.getItems()) {
            itemRepository.save(toItemJpaEntity(report.getId(), item));
        }

        return findById(report.getId()).orElse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccidentReport> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return accidentReportRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccidentReport> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return accidentReportRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccidentReport> findPage(Pageable pageable) {
        return accidentReportRepository.findAll(pageable).map(this::toDomain);
    }

    private AccidentReport toDomain(AccidentReportJpaEntity entity) {
        List<String> photoKeys = photoRepository.findByAccidentReportId(entity.getId()).stream()
                .map(AccidentReportPhotoJpaEntity::getPhotoObjectKey)
                .toList();

        List<AccidentReportItem> items = itemRepository.findByAccidentReportId(entity.getId()).stream()
                .map(this::toItemDomain)
                .toList();

        AccidentBudget budget = new AccidentBudget(
                Money.of(entity.getPartsSubtotalBrl()),
                Money.of(entity.getLaborFeeBrl()),
                Money.of(entity.getTowingFeeBrl()),
                Money.of(entity.getTotalEstimatedBrl()),
                items.size()
        );

        return AccidentReport.reconstitute(
                entity.getId(),
                entity.getIdempotencyKey(),
                entity.getContractId(),
                entity.getRiderId(),
                entity.getMotorcycleId(),
                entity.getVideoObjectKey(),
                entity.getVideoDurationSeconds(),
                entity.getVideoSizeBytes(),
                photoKeys,
                entity.isHasInjuries(),
                entity.isInvolvesThirdParty(),
                entity.getPoliceReportNumber(),
                entity.getDrivability(),
                entity.getReportedOdometerKm(),
                entity.getRiderNotes(),
                entity.getRiderToMechanicNotes(),
                entity.getStatus(),
                entity.getAiConfidence() != null ? entity.getAiConfidence().doubleValue() : null,
                entity.isStructuralRiskAlert(),
                entity.getAiTechnicalSummary(),
                budget,
                items,
                entity.getReviewedByMechanicId(),
                entity.getMechanicNotes(),
                entity.getCreatedAt(),
                entity.getConfirmedAt()
        );
    }

    private AccidentReportItem toItemDomain(AccidentReportItemJpaEntity entity) {
        return AccidentReportItem.reconstitute(
                entity.getId(),
                entity.getCatalogPartId(),
                entity.getPartNameSnapshot(),
                Money.of(entity.getUnitPriceSnapshotBrl()),
                entity.getQuantity(),
                entity.getVideoTimestampSeconds(),
                entity.isConfirmedByMechanic(),
                entity.getAddedBy()
        );
    }

    private AccidentReportJpaEntity toJpaEntity(AccidentReport report) {
        AccidentReportJpaEntity entity = new AccidentReportJpaEntity();
        entity.setId(report.getId());
        entity.setIdempotencyKey(report.getIdempotencyKey());
        entity.setContractId(report.getContractId());
        entity.setRiderId(report.getRiderId());
        entity.setMotorcycleId(report.getMotorcycleId());
        entity.setVideoObjectKey(report.getVideoObjectKey());
        entity.setVideoDurationSeconds(report.getVideoDurationSeconds());
        entity.setVideoSizeBytes(report.getVideoSizeBytes());
        entity.setHasInjuries(report.isHasInjuries());
        entity.setInvolvesThirdParty(report.isInvolvesThirdParty());
        entity.setPoliceReportNumber(report.getPoliceReportNumber());
        entity.setDrivability(report.getDrivability());
        entity.setReportedOdometerKm(report.getReportedOdometerKm());
        entity.setRiderNotes(report.getRiderNotes());
        entity.setRiderToMechanicNotes(report.getRiderToMechanicNotes());
        entity.setStatus(report.getStatus());
        entity.setAiConfidence(report.getAiConfidence() != null ? BigDecimal.valueOf(report.getAiConfidence()) : null);
        entity.setStructuralRiskAlert(report.isStructuralRiskAlert());
        entity.setAiTechnicalSummary(report.getAiTechnicalSummary());

        AccidentBudget budget = report.getBudget();
        entity.setPartsSubtotalBrl(budget != null && budget.partsSubtotal() != null ? budget.partsSubtotal().getAmount() : BigDecimal.ZERO);
        entity.setLaborFeeBrl(budget != null && budget.laborFee() != null ? budget.laborFee().getAmount() : BigDecimal.ZERO);
        entity.setTowingFeeBrl(budget != null && budget.towingFee() != null ? budget.towingFee().getAmount() : BigDecimal.ZERO);
        entity.setTotalEstimatedBrl(budget != null && budget.totalEstimated() != null ? budget.totalEstimated().getAmount() : BigDecimal.ZERO);

        entity.setReviewedByMechanicId(report.getReviewedByMechanicId());
        entity.setMechanicNotes(report.getMechanicNotes());
        entity.setCreatedAt(report.getCreatedAt());
        entity.setConfirmedAt(report.getConfirmedAt());
        return entity;
    }

    private AccidentReportItemJpaEntity toItemJpaEntity(UUID accidentReportId, AccidentReportItem item) {
        return new AccidentReportItemJpaEntity(
                item.getId(),
                accidentReportId,
                item.getCatalogPartId(),
                item.getPartName(),
                item.getUnitPrice().getAmount(),
                item.getQuantity(),
                item.getVideoTimestampSeconds(),
                item.isConfirmedByMechanic(),
                item.getAddedBy()
        );
    }
}
