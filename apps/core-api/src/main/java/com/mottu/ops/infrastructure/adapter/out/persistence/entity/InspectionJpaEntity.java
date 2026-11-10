package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspections")
public class InspectionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "motorcycle_id", nullable = false)
    private UUID motorcycleId;

    @Column(name = "photo_object_key", nullable = false, length = 255)
    private String photoObjectKey;

    @Column(name = "photo_sha256", nullable = false, length = 64)
    private String photoSha256;

    @Column(name = "photo_perceptual_hash", length = 64)
    private String photoPerceptualHash;

    @Column(name = "fraud_suspected", nullable = false)
    private boolean fraudSuspected;

    @Column(name = "fraud_reason", length = 200)
    private String fraudReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_context", nullable = false, length = 30)
    private ReportedOriginContext reportedContext;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_risk", nullable = false, length = 25)
    private SafetyRiskLevel safetyRisk;

    @Column(name = "reported_odometer_km", nullable = false)
    private int reportedOdometerKm;

    @Column(name = "rider_notes", length = 300)
    private String riderNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InspectionStatus status;

    @Column(name = "priority_weight")
    private Integer priorityWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_reason", length = 35)
    private QueuePriorityReason priorityReason;

    @Column(name = "ai_confidence", precision = 4, scale = 3)
    private BigDecimal aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_verdict", length = 25)
    private DamageVerdict aiVerdict;

    @Column(name = "ai_technical_summary", columnDefinition = "TEXT")
    private String aiTechnicalSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_verdict", length = 25)
    private DamageVerdict finalVerdict;

    @Column(name = "labor_fee_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborFeeBrl = BigDecimal.ZERO;

    @Column(name = "total_estimated_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEstimatedBrl = BigDecimal.ZERO;

    @Column(name = "customer_feedback_text", columnDefinition = "TEXT")
    private String customerFeedbackText;

    @Column(name = "contested_once", nullable = false)
    private boolean contestedOnce;

    @Column(name = "contestation_reason", length = 500)
    private String contestationReason;

    @Column(name = "reviewed_by_mechanic_id")
    private UUID reviewedByMechanicId;

    @Column(name = "mechanic_notes", length = 500)
    private String mechanicNotes;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "committee_evaluation_json", columnDefinition = "jsonb")
    private String committeeEvaluationJson;

    @Column(name = "presential_inspection_requested", nullable = false)
    private boolean presentialInspectionRequested = false;

    @Column(name = "presential_request_reason", length = 255)
    private String presentialRequestReason;

    public InspectionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public void setRiderId(UUID riderId) {
        this.riderId = riderId;
    }

    public UUID getMotorcycleId() {
        return motorcycleId;
    }

    public void setMotorcycleId(UUID motorcycleId) {
        this.motorcycleId = motorcycleId;
    }

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public void setPhotoObjectKey(String photoObjectKey) {
        this.photoObjectKey = photoObjectKey;
    }

    public String getPhotoSha256() {
        return photoSha256;
    }

    public void setPhotoSha256(String photoSha256) {
        this.photoSha256 = photoSha256;
    }

    public String getPhotoPerceptualHash() {
        return photoPerceptualHash;
    }

    public void setPhotoPerceptualHash(String photoPerceptualHash) {
        this.photoPerceptualHash = photoPerceptualHash;
    }

    public boolean isFraudSuspected() {
        return fraudSuspected;
    }

    public void setFraudSuspected(boolean fraudSuspected) {
        this.fraudSuspected = fraudSuspected;
    }

    public String getFraudReason() {
        return fraudReason;
    }

    public void setFraudReason(String fraudReason) {
        this.fraudReason = fraudReason;
    }

    public ReportedOriginContext getReportedContext() {
        return reportedContext;
    }

    public void setReportedContext(ReportedOriginContext reportedContext) {
        this.reportedContext = reportedContext;
    }

    public SafetyRiskLevel getSafetyRisk() {
        return safetyRisk;
    }

    public void setSafetyRisk(SafetyRiskLevel safetyRisk) {
        this.safetyRisk = safetyRisk;
    }

    public int getReportedOdometerKm() {
        return reportedOdometerKm;
    }

    public void setReportedOdometerKm(int reportedOdometerKm) {
        this.reportedOdometerKm = reportedOdometerKm;
    }

    public String getRiderNotes() {
        return riderNotes;
    }

    public void setRiderNotes(String riderNotes) {
        this.riderNotes = riderNotes;
    }

    public InspectionStatus getStatus() {
        return status;
    }

    public void setStatus(InspectionStatus status) {
        this.status = status;
    }

    public Integer getPriorityWeight() {
        return priorityWeight;
    }

    public void setPriorityWeight(Integer priorityWeight) {
        this.priorityWeight = priorityWeight;
    }

    public QueuePriorityReason getPriorityReason() {
        return priorityReason;
    }

    public void setPriorityReason(QueuePriorityReason priorityReason) {
        this.priorityReason = priorityReason;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(BigDecimal aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public DamageVerdict getAiVerdict() {
        return aiVerdict;
    }

    public void setAiVerdict(DamageVerdict aiVerdict) {
        this.aiVerdict = aiVerdict;
    }

    public String getAiTechnicalSummary() {
        return aiTechnicalSummary;
    }

    public void setAiTechnicalSummary(String aiTechnicalSummary) {
        this.aiTechnicalSummary = aiTechnicalSummary;
    }

    public DamageVerdict getFinalVerdict() {
        return finalVerdict;
    }

    public void setFinalVerdict(DamageVerdict finalVerdict) {
        this.finalVerdict = finalVerdict;
    }

    public BigDecimal getLaborFeeBrl() {
        return laborFeeBrl;
    }

    public void setLaborFeeBrl(BigDecimal laborFeeBrl) {
        this.laborFeeBrl = laborFeeBrl;
    }

    public BigDecimal getTotalEstimatedBrl() {
        return totalEstimatedBrl;
    }

    public void setTotalEstimatedBrl(BigDecimal totalEstimatedBrl) {
        this.totalEstimatedBrl = totalEstimatedBrl;
    }

    public String getCustomerFeedbackText() {
        return customerFeedbackText;
    }

    public void setCustomerFeedbackText(String customerFeedbackText) {
        this.customerFeedbackText = customerFeedbackText;
    }

    public boolean isContestedOnce() {
        return contestedOnce;
    }

    public void setContestedOnce(boolean contestedOnce) {
        this.contestedOnce = contestedOnce;
    }

    public String getContestationReason() {
        return contestationReason;
    }

    public void setContestationReason(String contestationReason) {
        this.contestationReason = contestationReason;
    }

    public UUID getReviewedByMechanicId() {
        return reviewedByMechanicId;
    }

    public void setReviewedByMechanicId(UUID reviewedByMechanicId) {
        this.reviewedByMechanicId = reviewedByMechanicId;
    }

    public String getMechanicNotes() {
        return mechanicNotes;
    }

    public void setMechanicNotes(String mechanicNotes) {
        this.mechanicNotes = mechanicNotes;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getCommitteeEvaluationJson() {
        return committeeEvaluationJson;
    }

    public void setCommitteeEvaluationJson(String committeeEvaluationJson) {
        this.committeeEvaluationJson = committeeEvaluationJson;
    }

    public boolean isPresentialInspectionRequested() {
        return presentialInspectionRequested;
    }

    public void setPresentialInspectionRequested(boolean presentialInspectionRequested) {
        this.presentialInspectionRequested = presentialInspectionRequested;
    }

    public String getPresentialRequestReason() {
        return presentialRequestReason;
    }

    public void setPresentialRequestReason(String presentialRequestReason) {
        this.presentialRequestReason = presentialRequestReason;
    }
}
