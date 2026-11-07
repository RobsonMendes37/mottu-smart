package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Inspection {
    private final UUID id;
    private final String idempotencyKey;
    private final UUID contractId;
    private final UUID riderId;
    private final UUID motorcycleId;
    private final String photoObjectKey;
    private final String photoSha256;
    private final String photoPerceptualHash;
    private boolean fraudSuspected;
    private String fraudReason;
    private final ReportedOriginContext reportedContext;
    private final SafetyRiskLevel safetyRisk;
    private final int reportedOdometerKm;
    private final String riderNotes;
    private InspectionStatus status;
    private Integer priorityWeight;
    private QueuePriorityReason priorityReason;
    private BigDecimal aiConfidence;
    private DamageVerdict aiVerdict;
    private String aiTechnicalSummary;
    private DamageVerdict finalVerdict;
    private Money laborFee;
    private Money totalEstimated;
    private String customerFeedbackText;
    private boolean contestedOnce;
    private String contestationReason;
    private UUID reviewedByMechanicId;
    private String mechanicNotes;
    private int retryCount;
    private Instant nextRetryAt;
    private final Instant createdAt;
    private Instant resolvedAt;
    private final List<InspectionItem> items;

    public Inspection(
            UUID id,
            String idempotencyKey,
            UUID contractId,
            UUID riderId,
            UUID motorcycleId,
            String photoObjectKey,
            String photoSha256,
            String photoPerceptualHash,
            boolean fraudSuspected,
            String fraudReason,
            ReportedOriginContext reportedContext,
            SafetyRiskLevel safetyRisk,
            int reportedOdometerKm,
            String riderNotes,
            InspectionStatus status,
            Integer priorityWeight,
            QueuePriorityReason priorityReason,
            BigDecimal aiConfidence,
            DamageVerdict aiVerdict,
            String aiTechnicalSummary,
            DamageVerdict finalVerdict,
            Money laborFee,
            Money totalEstimated,
            String customerFeedbackText,
            boolean contestedOnce,
            String contestationReason,
            UUID reviewedByMechanicId,
            String mechanicNotes,
            int retryCount,
            Instant nextRetryAt,
            Instant createdAt,
            Instant resolvedAt,
            List<InspectionItem> items
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new DomainValidationException("Idempotency key cannot be null or blank");
        }
        if (contractId == null) {
            throw new DomainValidationException("Contract ID cannot be null");
        }
        if (riderId == null) {
            throw new DomainValidationException("Rider ID cannot be null");
        }
        if (motorcycleId == null) {
            throw new DomainValidationException("Motorcycle ID cannot be null");
        }
        if (photoObjectKey == null || photoObjectKey.isBlank()) {
            throw new DomainValidationException("Photo object key cannot be null or blank");
        }
        if (photoSha256 == null || photoSha256.length() != 64) {
            throw new DomainValidationException("Photo SHA-256 must be exactly 64 characters");
        }
        if (reportedContext == null) {
            throw new DomainValidationException("Reported context cannot be null");
        }
        if (safetyRisk == null) {
            throw new DomainValidationException("Safety risk level cannot be null");
        }
        if (reportedOdometerKm < 0) {
            throw new DomainValidationException("Reported odometer cannot be negative");
        }
        if (riderNotes != null && riderNotes.length() > 300) {
            throw new DomainValidationException("Rider notes cannot exceed 300 characters");
        }
        if (createdAt == null) {
            throw new DomainValidationException("Created at timestamp cannot be null");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.contractId = contractId;
        this.riderId = riderId;
        this.motorcycleId = motorcycleId;
        this.photoObjectKey = photoObjectKey;
        this.photoSha256 = photoSha256.toLowerCase();
        this.photoPerceptualHash = photoPerceptualHash;
        this.fraudSuspected = fraudSuspected;
        this.fraudReason = fraudReason;
        this.reportedContext = reportedContext;
        this.safetyRisk = safetyRisk;
        this.reportedOdometerKm = reportedOdometerKm;
        this.riderNotes = riderNotes;
        this.status = status != null ? status : (fraudSuspected ? InspectionStatus.HUMAN_TRIAGE : InspectionStatus.QUEUED_AI);
        this.priorityReason = priorityReason != null ? priorityReason : (fraudSuspected ? QueuePriorityReason.FRAUD_SUSPICION : null);
        this.priorityWeight = priorityWeight != null ? priorityWeight : (this.priorityReason != null ? this.priorityReason.getWeight() : null);
        this.aiConfidence = aiConfidence;
        this.aiVerdict = aiVerdict;
        this.aiTechnicalSummary = aiTechnicalSummary;
        this.finalVerdict = finalVerdict;
        this.laborFee = laborFee != null ? laborFee : Money.ZERO;
        this.totalEstimated = totalEstimated != null ? totalEstimated : Money.ZERO;
        this.customerFeedbackText = customerFeedbackText;
        this.contestedOnce = contestedOnce;
        this.contestationReason = contestationReason;
        this.reviewedByMechanicId = reviewedByMechanicId;
        this.mechanicNotes = mechanicNotes;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public static Inspection createInitial(
            String idempotencyKey,
            UUID contractId,
            UUID riderId,
            UUID motorcycleId,
            String photoObjectKey,
            String photoSha256,
            String photoPerceptualHash,
            boolean fraudSuspected,
            String fraudReason,
            ReportedOriginContext reportedContext,
            SafetyRiskLevel safetyRisk,
            int reportedOdometerKm,
            String riderNotes,
            Instant createdAt
    ) {
        return new Inspection(
                UUID.randomUUID(),
                idempotencyKey,
                contractId,
                riderId,
                motorcycleId,
                photoObjectKey,
                photoSha256,
                photoPerceptualHash,
                fraudSuspected,
                fraudReason,
                reportedContext,
                safetyRisk,
                reportedOdometerKm,
                riderNotes,
                fraudSuspected ? InspectionStatus.HUMAN_TRIAGE : InspectionStatus.QUEUED_AI,
                fraudSuspected ? QueuePriorityReason.FRAUD_SUSPICION.getWeight() : null,
                fraudSuspected ? QueuePriorityReason.FRAUD_SUSPICION : null,
                null, null, null, null,
                Money.ZERO, Money.ZERO,
                null, false, null, null, null, 0, null,
                createdAt != null ? createdAt : Instant.now(),
                null,
                new ArrayList<>()
        );
    }

    public void flagFraud(String reason) {
        this.fraudSuspected = true;
        this.fraudReason = reason;
        this.status = InspectionStatus.HUMAN_TRIAGE;
        this.priorityReason = QueuePriorityReason.FRAUD_SUSPICION;
        this.priorityWeight = QueuePriorityReason.FRAUD_SUSPICION.getWeight();
    }

    public void applyPreExistingExemption() {
        if (this.fraudSuspected) {
            throw new BusinessRuleViolationException("Cannot apply pre-existing exemption to fraudulent inspection");
        }
        this.finalVerdict = DamageVerdict.PRE_EXISTENTE;
        this.laborFee = Money.ZERO;
        this.totalEstimated = Money.ZERO;
    }

    public void autoResolve(DamageVerdict verdict, BigDecimal confidence, Money laborFee, Money total, String summary, Instant resolvedAt) {
        if (this.fraudSuspected) {
            throw new BusinessRuleViolationException("Cannot auto-resolve inspection flagged for fraud");
        }
        this.aiVerdict = verdict;
        this.aiConfidence = confidence;
        this.finalVerdict = verdict;
        this.laborFee = laborFee != null ? laborFee : Money.ZERO;
        this.totalEstimated = total != null ? total : Money.ZERO;
        this.aiTechnicalSummary = summary;
        this.status = InspectionStatus.AUTO_RESOLVED;
        this.resolvedAt = resolvedAt;
    }

    public void routeToHumanTriage(QueuePriorityReason reason) {
        this.status = InspectionStatus.HUMAN_TRIAGE;
        this.priorityReason = reason;
        this.priorityWeight = (this.safetyRisk == SafetyRiskLevel.CRITICAL_STOPPED) ? 1 : reason.getWeight();
    }

    public void contest(String reason) {
        if (this.contestedOnce) {
            throw new BusinessRuleViolationException("Inspection has already been contested once");
        }
        if (this.status != InspectionStatus.AUTO_RESOLVED) {
            throw new BusinessRuleViolationException("Only AUTO_RESOLVED inspections can be contested");
        }
        if (reason == null || reason.trim().length() < 10) {
            throw new DomainValidationException("Contestation reason must have at least 10 characters");
        }
        this.contestedOnce = true;
        this.contestationReason = reason.trim();
        this.status = InspectionStatus.HUMAN_TRIAGE;
        this.priorityReason = QueuePriorityReason.RIDER_CONTESTATION;
        this.priorityWeight = (this.safetyRisk == SafetyRiskLevel.CRITICAL_STOPPED) ? 1 : 2;
    }

    public void resolveByMechanic(UUID mechanicId, DamageVerdict verdict, Money laborFee, Money total, String mechanicNotes, String feedbackText, Instant resolvedAt) {
        if (this.status != InspectionStatus.HUMAN_TRIAGE) {
            throw new BusinessRuleViolationException("Only inspections in HUMAN_TRIAGE can be resolved by a mechanic");
        }
        this.reviewedByMechanicId = Objects.requireNonNull(mechanicId, "Mechanic ID cannot be null");
        this.finalVerdict = Objects.requireNonNull(verdict, "Final verdict cannot be null");
        this.laborFee = Objects.requireNonNull(laborFee, "Labor fee cannot be null");
        this.totalEstimated = Objects.requireNonNull(total, "Total estimated cannot be null");
        this.mechanicNotes = mechanicNotes;
        this.customerFeedbackText = feedbackText;
        this.status = InspectionStatus.HUMAN_RESOLVED;
        this.resolvedAt = resolvedAt != null ? resolvedAt : Instant.now();
    }

    public void scheduleAppointment() {
        if (this.status != InspectionStatus.AUTO_RESOLVED && this.status != InspectionStatus.HUMAN_RESOLVED) {
            throw new BusinessRuleViolationException("Only resolved inspections can be scheduled for yard visit");
        }
        this.status = InspectionStatus.SCHEDULED;
    }

    public void addItem(InspectionItem item) {
        Objects.requireNonNull(item, "Inspection item cannot be null");
        this.items.add(item);
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

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public String getPhotoSha256() {
        return photoSha256;
    }

    public String getPhotoPerceptualHash() {
        return photoPerceptualHash;
    }

    public boolean isFraudSuspected() {
        return fraudSuspected;
    }

    public String getFraudReason() {
        return fraudReason;
    }

    public ReportedOriginContext getReportedContext() {
        return reportedContext;
    }

    public SafetyRiskLevel getSafetyRisk() {
        return safetyRisk;
    }

    public int getReportedOdometerKm() {
        return reportedOdometerKm;
    }

    public String getRiderNotes() {
        return riderNotes;
    }

    public InspectionStatus getStatus() {
        return status;
    }

    public Integer getPriorityWeight() {
        return priorityWeight;
    }

    public QueuePriorityReason getPriorityReason() {
        return priorityReason;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public DamageVerdict getAiVerdict() {
        return aiVerdict;
    }

    public String getAiTechnicalSummary() {
        return aiTechnicalSummary;
    }

    public DamageVerdict getFinalVerdict() {
        return finalVerdict;
    }

    public Money getLaborFee() {
        return laborFee;
    }

    public Money getTotalEstimated() {
        return totalEstimated;
    }

    public String getCustomerFeedbackText() {
        return customerFeedbackText;
    }

    public boolean isContestedOnce() {
        return contestedOnce;
    }

    public String getContestationReason() {
        return contestationReason;
    }

    public UUID getReviewedByMechanicId() {
        return reviewedByMechanicId;
    }

    public String getMechanicNotes() {
        return mechanicNotes;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public List<InspectionItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
