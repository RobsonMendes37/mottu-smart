package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.VehicleDrivability;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.service.AccidentBudgetCalculator;
import com.mottu.ops.domain.service.AccidentStateMachine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AccidentReport {

    public static final int MAX_VIDEO_DURATION_SECONDS = 30;
    public static final long MAX_VIDEO_SIZE_BYTES = 30 * 1024 * 1024L; // 30 MB

    private final UUID id;
    private final String idempotencyKey;
    private final UUID contractId;
    private final UUID riderId;
    private final UUID motorcycleId;
    private final String videoObjectKey;
    private final int videoDurationSeconds;
    private final long videoSizeBytes;
    private final List<String> photoObjectKeys;
    private final boolean hasInjuries;
    private final boolean involvesThirdParty;
    private final String policeReportNumber;
    private final VehicleDrivability drivability;
    private final int reportedOdometerKm;
    private final String riderNotes;
    private String riderToMechanicNotes;
    private AccidentReportStatus status;
    private Double aiConfidence;
    private boolean structuralRiskAlert;
    private String aiTechnicalSummary;
    private AccidentBudget budget;
    private final List<AccidentReportItem> items;
    private UUID reviewedByMechanicId;
    private String mechanicNotes;
    private final Instant createdAt;
    private Instant confirmedAt;

    private AccidentReport(
            UUID id,
            String idempotencyKey,
            UUID contractId,
            UUID riderId,
            UUID motorcycleId,
            String videoObjectKey,
            int videoDurationSeconds,
            long videoSizeBytes,
            List<String> photoObjectKeys,
            boolean hasInjuries,
            boolean involvesThirdParty,
            String policeReportNumber,
            VehicleDrivability drivability,
            int reportedOdometerKm,
            String riderNotes,
            String riderToMechanicNotes,
            AccidentReportStatus status,
            Double aiConfidence,
            boolean structuralRiskAlert,
            String aiTechnicalSummary,
            AccidentBudget budget,
            List<AccidentReportItem> items,
            UUID reviewedByMechanicId,
            String mechanicNotes,
            Instant createdAt,
            Instant confirmedAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey cannot be null");
        this.contractId = Objects.requireNonNull(contractId, "contractId cannot be null");
        this.riderId = Objects.requireNonNull(riderId, "riderId cannot be null");
        this.motorcycleId = Objects.requireNonNull(motorcycleId, "motorcycleId cannot be null");
        this.videoObjectKey = Objects.requireNonNull(videoObjectKey, "videoObjectKey cannot be null");

        if (videoDurationSeconds > MAX_VIDEO_DURATION_SECONDS) {
            throw new BusinessRuleViolationException("Video duration cannot exceed 30 seconds");
        }
        if (videoSizeBytes > MAX_VIDEO_SIZE_BYTES) {
            throw new BusinessRuleViolationException("Video size cannot exceed 30 MB");
        }

        this.videoDurationSeconds = videoDurationSeconds;
        this.videoSizeBytes = videoSizeBytes;
        this.photoObjectKeys = photoObjectKeys != null ? new ArrayList<>(photoObjectKeys) : new ArrayList<>();
        this.hasInjuries = hasInjuries;
        this.involvesThirdParty = involvesThirdParty;
        this.policeReportNumber = policeReportNumber;
        this.drivability = Objects.requireNonNull(drivability, "drivability cannot be null");
        this.reportedOdometerKm = reportedOdometerKm;
        this.riderNotes = riderNotes;
        this.riderToMechanicNotes = riderToMechanicNotes;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.aiConfidence = aiConfidence;
        this.structuralRiskAlert = structuralRiskAlert;
        this.aiTechnicalSummary = aiTechnicalSummary;
        this.budget = budget != null ? budget : AccidentBudget.zero();
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.reviewedByMechanicId = reviewedByMechanicId;
        this.mechanicNotes = mechanicNotes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.confirmedAt = confirmedAt;
    }

    public static AccidentReport createInitial(
            String idempotencyKey,
            UUID contractId,
            UUID riderId,
            UUID motorcycleId,
            String videoObjectKey,
            int videoDurationSeconds,
            long videoSizeBytes,
            List<String> photoObjectKeys,
            boolean hasInjuries,
            boolean involvesThirdParty,
            String policeReportNumber,
            VehicleDrivability drivability,
            int reportedOdometerKm,
            String riderNotes,
            String riderToMechanicNotes
    ) {
        return new AccidentReport(
                UUID.randomUUID(),
                idempotencyKey,
                contractId,
                riderId,
                motorcycleId,
                videoObjectKey,
                videoDurationSeconds,
                videoSizeBytes,
                photoObjectKeys,
                hasInjuries,
                involvesThirdParty,
                policeReportNumber,
                drivability,
                reportedOdometerKm,
                riderNotes,
                riderToMechanicNotes,
                AccidentReportStatus.QUEUED_AI,
                null,
                false,
                null,
                AccidentBudget.zero(),
                new ArrayList<>(),
                null,
                null,
                Instant.now(),
                null
        );
    }

    public static AccidentReport reconstitute(
            UUID id,
            String idempotencyKey,
            UUID contractId,
            UUID riderId,
            UUID motorcycleId,
            String videoObjectKey,
            int videoDurationSeconds,
            long videoSizeBytes,
            List<String> photoObjectKeys,
            boolean hasInjuries,
            boolean involvesThirdParty,
            String policeReportNumber,
            VehicleDrivability drivability,
            int reportedOdometerKm,
            String riderNotes,
            String riderToMechanicNotes,
            AccidentReportStatus status,
            Double aiConfidence,
            boolean structuralRiskAlert,
            String aiTechnicalSummary,
            AccidentBudget budget,
            List<AccidentReportItem> items,
            UUID reviewedByMechanicId,
            String mechanicNotes,
            Instant createdAt,
            Instant confirmedAt
    ) {
        return new AccidentReport(
                id,
                idempotencyKey,
                contractId,
                riderId,
                motorcycleId,
                videoObjectKey,
                videoDurationSeconds,
                videoSizeBytes,
                photoObjectKeys,
                hasInjuries,
                involvesThirdParty,
                policeReportNumber,
                drivability,
                reportedOdometerKm,
                riderNotes,
                riderToMechanicNotes,
                status,
                aiConfidence,
                structuralRiskAlert,
                aiTechnicalSummary,
                budget,
                items,
                reviewedByMechanicId,
                mechanicNotes,
                createdAt,
                confirmedAt
        );
    }

    public void applyAiEstimation(
            Double confidence,
            boolean structuralRisk,
            String technicalSummary,
            List<AccidentReportItem> detectedItems,
            AccidentBudgetCalculator calculator,
            AccidentStateMachine stateMachine
    ) {
        stateMachine.validateTransition(this.status, AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP);
        this.status = AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP;
        this.aiConfidence = confidence;
        this.structuralRiskAlert = structuralRisk;
        this.aiTechnicalSummary = technicalSummary;
        this.items.clear();
        if (detectedItems != null) {
            this.items.addAll(detectedItems);
        }
        this.budget = calculator.calculate(this.items, this.drivability, null, null, null);
    }

    public void markAwaitingAiRecovery(AccidentStateMachine stateMachine) {
        stateMachine.validateTransition(this.status, AccidentReportStatus.AWAITING_AI_RECOVERY);
        this.status = AccidentReportStatus.AWAITING_AI_RECOVERY;
    }

    public void confirmByMechanic(
            UUID mechanicId,
            List<AccidentReportItem> confirmedItems,
            List<AccidentReportItem> additionalItems,
            String notes,
            AccidentBudgetCalculator calculator,
            AccidentStateMachine stateMachine
    ) {
        stateMachine.validateTransition(this.status, AccidentReportStatus.WORKSHOP_CONFIRMED);
        this.reviewedByMechanicId = Objects.requireNonNull(mechanicId, "mechanicId cannot be null");
        this.mechanicNotes = notes;
        this.confirmedAt = Instant.now();

        this.items.clear();
        if (confirmedItems != null) {
            this.items.addAll(confirmedItems);
        }
        if (additionalItems != null) {
            this.items.addAll(additionalItems);
        }

        this.budget = calculator.calculate(this.items, this.drivability, null, null, null);
        this.status = AccidentReportStatus.WORKSHOP_CONFIRMED;
    }

    public void updateRiderToMechanicNotes(String notes) {
        this.riderToMechanicNotes = notes;
    }

    public boolean shouldLockMotorcycle() {
        return this.drivability == VehicleDrivability.NEEDS_TOWING;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public UUID getMotorcycleId() {
        return motorcycleId;
    }

    public String getVideoObjectKey() {
        return videoObjectKey;
    }

    public int getVideoDurationSeconds() {
        return videoDurationSeconds;
    }

    public long getVideoSizeBytes() {
        return videoSizeBytes;
    }

    public List<String> getPhotoObjectKeys() {
        return Collections.unmodifiableList(photoObjectKeys);
    }

    public boolean isHasInjuries() {
        return hasInjuries;
    }

    public boolean isInvolvesThirdParty() {
        return involvesThirdParty;
    }

    public boolean getInvolvesThirdParty() {
        return involvesThirdParty;
    }

    public String getPoliceReportNumber() {
        return policeReportNumber;
    }

    public VehicleDrivability getDrivability() {
        return drivability;
    }

    public int getReportedOdometerKm() {
        return reportedOdometerKm;
    }

    public String getRiderNotes() {
        return riderNotes;
    }

    public String getRiderToMechanicNotes() {
        return riderToMechanicNotes;
    }

    public AccidentReportStatus getStatus() {
        return status;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public boolean isStructuralRiskAlert() {
        return structuralRiskAlert;
    }

    public String getAiTechnicalSummary() {
        return aiTechnicalSummary;
    }

    public AccidentBudget getBudget() {
        return budget;
    }

    public List<AccidentReportItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public UUID getReviewedByMechanicId() {
        return reviewedByMechanicId;
    }

    public String getMechanicNotes() {
        return mechanicNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}

