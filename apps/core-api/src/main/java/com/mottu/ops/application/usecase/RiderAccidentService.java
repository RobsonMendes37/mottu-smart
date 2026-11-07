package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.RiderAccidentUseCase;
import com.mottu.ops.application.port.out.AccidentQueuePort;
import com.mottu.ops.application.port.out.AccidentReportRepositoryPort;
import com.mottu.ops.application.port.out.StoragePort;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.model.AccidentReport;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RiderAccidentService implements RiderAccidentUseCase {

    private static final Logger log = LoggerFactory.getLogger(RiderAccidentService.class);

    private final AccidentReportRepositoryPort accidentReportRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final StoragePort storagePort;
    private final AccidentQueuePort accidentQueuePort;
    private final SseEmitterRegistry sseEmitterRegistry;

    public RiderAccidentService(
            AccidentReportRepositoryPort accidentReportRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            StoragePort storagePort,
            AccidentQueuePort accidentQueuePort,
            SseEmitterRegistry sseEmitterRegistry
    ) {
        this.accidentReportRepository = accidentReportRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.storagePort = storagePort;
        this.accidentQueuePort = accidentQueuePort;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @Override
    public AccidentVideoUploadUrlResponse generateVideoUploadUrl(AccidentVideoUploadUrlRequest request) {
        UploadUrlResponse upload = storagePort.generatePresignedVideoUploadUrl(
                request.contentType(),
                request.fileSizeBytes(),
                request.durationSeconds()
        );
        return new AccidentVideoUploadUrlResponse(upload.uploadUrl(), upload.photoObjectKey(), upload.expiresInSeconds());
    }

    @Override
    public AccidentPhotoUploadUrlResponse generatePhotoUploadUrl(AccidentPhotoUploadUrlRequest request) {
        UploadUrlResponse upload = storagePort.generatePresignedAccidentPhotoUploadUrl(
                request.contentType(),
                request.fileSizeBytes()
        );
        return new AccidentPhotoUploadUrlResponse(upload.uploadUrl(), upload.photoObjectKey(), upload.expiresInSeconds());
    }

    @Override
    @Transactional
    public CreateAccidentReportResponse createAccidentReport(UUID riderId, String idempotencyKey, CreateAccidentReportCommand command) {
        // 1. Idempotency Check
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        Optional<AccidentReport> existing = accidentReportRepository.findByIdempotencyKey(effectiveKey);
        if (existing.isPresent()) {
            AccidentReport report = existing.get();
            MotorcycleJpaEntity moto = motorcycleRepository.findById(report.getMotorcycleId()).orElse(null);
            MotorcycleStatus motoStatus = moto != null ? moto.getStatus() : MotorcycleStatus.AVAILABLE;
            return new CreateAccidentReportResponse(report.getId(), report.getStatus(), motoStatus);
        }

        // 2. Active Rental Contract & Motorcycle
        RentalContractJpaEntity contract = rentalContractRepository.findByRiderIdAndActiveTrue(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum contrato ativo encontrado para o entregador"));

        MotorcycleJpaEntity motorcycle = motorcycleRepository.findById(contract.getMotorcycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Motocicleta do contrato não encontrada"));

        // 3. Create initial accident report
        AccidentReport report = AccidentReport.createInitial(
                effectiveKey,
                contract.getId(),
                riderId,
                motorcycle.getId(),
                command.videoObjectKey(),
                command.videoDurationSeconds(),
                command.videoSizeBytes(),
                command.photoObjectKeys(),
                command.hasInjuries(),
                command.involvesThirdParty(),
                command.policeReportNumber(),
                command.drivability(),
                command.reportedOdometerKm(),
                command.riderNotes(),
                command.riderToMechanicNotes()
        );

        // 4. Lock motorcycle if needs towing
        if (report.shouldLockMotorcycle()) {
            motorcycle.setStatus(MotorcycleStatus.IN_MAINTENANCE);
            motorcycleRepository.save(motorcycle);
            log.info("Motorcycle {} set to IN_MAINTENANCE due to accident report {}", motorcycle.getId(), report.getId());
        }

        // 5. Persist
        report = accidentReportRepository.save(report);

        // 6. Enqueue job for Python intelligence worker
        AccidentJobPayloadDto job = new AccidentJobPayloadDto(
                report.getId(),
                UUID.randomUUID(),
                report.getVideoObjectKey(),
                report.getPhotoObjectKeys(),
                motorcycle.getModel(),
                report.getDrivability(),
                report.getRiderNotes()
        );
        accidentQueuePort.publishJob(job);

        return new CreateAccidentReportResponse(report.getId(), report.getStatus(), motorcycle.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public RiderAccidentReportDetailsResponse getAccidentDetails(UUID riderId, UUID accidentReportId) {
        AccidentReport report = accidentReportRepository.findById(accidentReportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de acidente não encontrado: " + accidentReportId));

        if (!report.getRiderId().equals(riderId)) {
            throw new ResourceNotFoundException("Relato de acidente não encontrado: " + accidentReportId);
        }

        String videoDownloadUrl = storagePort.generatePresignedDownloadUrl(report.getVideoObjectKey());

        List<AccidentItemResponseDto> itemDtos = report.getItems().stream()
                .map(item -> new AccidentItemResponseDto(
                        item.getCatalogPartId(),
                        item.getPartName(),
                        item.getUnitPrice().getAmount(),
                        item.getQuantity(),
                        item.getVideoTimestampSeconds()
                ))
                .toList();

        return new RiderAccidentReportDetailsResponse(
                report.getId(),
                report.getStatus(),
                videoDownloadUrl,
                report.getDrivability(),
                report.isHasInjuries(),
                report.isStructuralRiskAlert(),
                report.getAiConfidence(),
                report.getBudget().partsSubtotal().getAmount(),
                report.getBudget().laborFee().getAmount(),
                report.getBudget().towingFee().getAmount(),
                report.getBudget().totalEstimated().getAmount(),
                itemDtos,
                report.getRiderToMechanicNotes(),
                report.getCreatedAt()
        );
    }

    @Override
    public SseEmitter subscribeLiveStatus(UUID accidentReportId) {
        AccidentReport report = accidentReportRepository.findById(accidentReportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relato de acidente não encontrado: " + accidentReportId));
        return sseEmitterRegistry.registerAccidentEmitter(accidentReportId, report.getStatus());
    }
}

