package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.MechanicAccidentUseCase;
import com.mottu.ops.application.port.out.AccidentReportRepositoryPort;
import com.mottu.ops.application.port.out.StoragePort;
import com.mottu.ops.domain.model.AccidentReport;
import com.mottu.ops.domain.model.AccidentReportItem;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.domain.service.AccidentBudgetCalculator;
import com.mottu.ops.domain.service.AccidentStateMachine;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.AnalyticsOutboxJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.CatalogPartJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataAnalyticsOutboxRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataCatalogPartRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class MechanicAccidentService implements MechanicAccidentUseCase {

    private static final Logger log = LoggerFactory.getLogger(MechanicAccidentService.class);

    private final AccidentReportRepositoryPort accidentReportRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataCatalogPartRepository catalogPartRepository;
    private final SpringDataAnalyticsOutboxRepository outboxRepository;
    private final StoragePort storagePort;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;
    private final AccidentBudgetCalculator budgetCalculator = new AccidentBudgetCalculator();
    private final AccidentStateMachine stateMachine = new AccidentStateMachine();

    public MechanicAccidentService(
            AccidentReportRepositoryPort accidentReportRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataUserRepository userRepository,
            SpringDataCatalogPartRepository catalogPartRepository,
            SpringDataAnalyticsOutboxRepository outboxRepository,
            StoragePort storagePort,
            SseEmitterRegistry sseEmitterRegistry,
            ObjectMapper objectMapper
    ) {
        this.accidentReportRepository = accidentReportRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.userRepository = userRepository;
        this.catalogPartRepository = catalogPartRepository;
        this.outboxRepository = outboxRepository;
        this.storagePort = storagePort;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MechanicAccidentQueueItemDto> getAccidentsQueue(Pageable pageable) {
        Page<AccidentReport> page = accidentReportRepository.findPage(pageable);

        return page.map(report -> {
            MotorcycleJpaEntity moto = motorcycleRepository.findById(report.getMotorcycleId()).orElse(null);
            UserJpaEntity rider = userRepository.findById(report.getRiderId()).orElse(null);

            String plate = moto != null ? moto.getPlate() : "N/A";
            var model = moto != null ? moto.getModel() : null;
            String riderName = rider != null ? rider.getFullName() : "Entregador";

            return new MechanicAccidentQueueItemDto(
                    report.getId(),
                    plate,
                    model,
                    riderName,
                    report.getDrivability(),
                    report.isHasInjuries(),
                    report.getBudget().totalEstimated().getAmount(),
                    report.getItems().size(),
                    report.getStatus(),
                    report.getCreatedAt()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public MechanicAccidentDetailsResponse getAccidentDetails(UUID accidentReportId) {
        AccidentReport report = accidentReportRepository.findById(accidentReportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de acidente não encontrado: " + accidentReportId));

        MotorcycleJpaEntity moto = motorcycleRepository.findById(report.getMotorcycleId()).orElse(null);
        UserJpaEntity rider = userRepository.findById(report.getRiderId()).orElse(null);

        String plate = moto != null ? moto.getPlate() : "N/A";
        var model = moto != null ? moto.getModel() : null;
        String riderName = rider != null ? rider.getFullName() : "Entregador";
        String riderPhone = rider != null ? rider.getPhone() : "";

        String videoDownloadUrl = storagePort.generatePresignedDownloadUrl(report.getVideoObjectKey());
        List<String> photoDownloadUrls = report.getPhotoObjectKeys().stream()
                .map(storagePort::generatePresignedDownloadUrl)
                .toList();

        List<AccidentItemResponseDto> itemDtos = report.getItems().stream()
                .map(item -> new AccidentItemResponseDto(
                        item.getCatalogPartId(),
                        item.getPartName(),
                        item.getUnitPrice().getAmount(),
                        item.getQuantity(),
                        item.getVideoTimestampSeconds()
                ))
                .toList();

        return new MechanicAccidentDetailsResponse(
                report.getId(),
                report.getStatus(),
                plate,
                model,
                riderName,
                riderPhone,
                videoDownloadUrl,
                photoDownloadUrls,
                report.getDrivability(),
                report.isHasInjuries(),
                report.isInvolvesThirdParty(),
                report.getPoliceReportNumber(),
                report.getReportedOdometerKm(),
                report.getRiderNotes(),
                report.getRiderToMechanicNotes(),
                report.getAiConfidence(),
                report.isStructuralRiskAlert(),
                report.getAiTechnicalSummary(),
                report.getBudget().partsSubtotal().getAmount(),
                report.getBudget().laborFee().getAmount(),
                report.getBudget().towingFee().getAmount(),
                report.getBudget().totalEstimated().getAmount(),
                itemDtos,
                report.getCreatedAt(),
                report.getConfirmedAt(),
                report.getMechanicNotes()
        );
    }

    @Override
    @Transactional
    public ConfirmAccidentReportResponse confirmAccident(UUID mechanicId, UUID accidentReportId, ConfirmAccidentReportCommand command) {
        AccidentReport report = accidentReportRepository.findById(accidentReportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de acidente não encontrado: " + accidentReportId));

        List<AccidentReportItem> confirmedItems = new ArrayList<>();
        if (command.confirmedPartIds() != null && !command.confirmedPartIds().isEmpty()) {
            Set<UUID> confirmedSet = new HashSet<>(command.confirmedPartIds());
            for (AccidentReportItem item : report.getItems()) {
                if (confirmedSet.contains(item.getId()) || confirmedSet.contains(item.getCatalogPartId())) {
                    confirmedItems.add(item);
                }
            }
        } else {
            // Se não especificou filtro, confirma todos os itens existentes
            confirmedItems.addAll(report.getItems());
        }

        List<AccidentReportItem> additionalItems = new ArrayList<>();
        if (command.additionalCatalogPartIds() != null) {
            for (UUID partId : command.additionalCatalogPartIds()) {
                CatalogPartJpaEntity part = catalogPartRepository.findById(partId)
                        .orElseThrow(() -> new ResourceNotFoundException("Peça de catálogo não encontrada: " + partId));
                additionalItems.add(AccidentReportItem.createMechanicItem(
                        part.getId(),
                        part.getPartName(),
                        Money.of(part.getUnitPriceBrl()),
                        1
                ));
            }
        }

        report.confirmByMechanic(mechanicId, confirmedItems, additionalItems, command.mechanicNotes(), budgetCalculator, stateMachine);
        accidentReportRepository.save(report);

        // Grava na analytics_outbox
        try {
            Map<String, Object> outboxPayload = Map.of(
                    "accidentReportId", report.getId(),
                    "riderId", report.getRiderId(),
                    "motorcycleId", report.getMotorcycleId(),
                    "status", report.getStatus().name(),
                    "totalEstimatedBrl", report.getBudget().totalEstimated().getAmount(),
                    "partsCount", report.getItems().size(),
                    "confirmedAt", report.getConfirmedAt().toString()
            );
            String payloadJson = objectMapper.writeValueAsString(outboxPayload);
            AnalyticsOutboxJpaEntity outbox = new AnalyticsOutboxJpaEntity(
                    UUID.randomUUID(),
                    null,
                    report.getId(),
                    payloadJson,
                    false,
                    Instant.now(),
                    null
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Erro serializando outbox para acidente {}", report.getId(), e);
        }

        sseEmitterRegistry.emitAccidentStatusChange(report.getId(), report.getStatus());
        log.info("Accident report {} confirmed by mechanic {}", report.getId(), mechanicId);

        return new ConfirmAccidentReportResponse(
                report.getId(),
                report.getStatus(),
                report.getBudget().totalEstimated().getAmount(),
                report.getConfirmedAt()
        );
    }
}
