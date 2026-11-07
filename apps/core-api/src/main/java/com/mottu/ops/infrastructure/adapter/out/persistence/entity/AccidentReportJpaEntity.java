package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.enums.VehicleDrivability;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accident_reports")
public class AccidentReportJpaEntity {

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

    @Column(name = "video_object_key", nullable = false, length = 255)
    private String videoObjectKey;

    @Column(name = "video_duration_seconds", nullable = false)
    private int videoDurationSeconds;

    @Column(name = "video_size_bytes", nullable = false)
    private long videoSizeBytes;

    @Column(name = "has_injuries", nullable = false)
    private boolean hasInjuries;

    @Column(name = "involves_third_party", nullable = false)
    private boolean involvesThirdParty;

    @Column(name = "police_report_number", length = 60)
    private String policeReportNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "drivability", nullable = false, length = 20)
    private VehicleDrivability drivability;

    @Column(name = "reported_odometer_km", nullable = false)
    private int reportedOdometerKm;

    @Column(name = "rider_notes", length = 500)
    private String riderNotes;

    @Column(name = "rider_to_mechanic_notes", length = 500)
    private String riderToMechanicNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 35)
    private AccidentReportStatus status;

    @Column(name = "ai_confidence", precision = 4, scale = 3)
    private BigDecimal aiConfidence;

    @Column(name = "structural_risk_alert", nullable = false)
    private boolean structuralRiskAlert;

    @Column(name = "ai_technical_summary", columnDefinition = "TEXT")
    private String aiTechnicalSummary;

    @Column(name = "parts_subtotal_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal partsSubtotalBrl;

    @Column(name = "labor_fee_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal laborFeeBrl;

    @Column(name = "towing_fee_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal towingFeeBrl;

    @Column(name = "total_estimated_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEstimatedBrl;

    @Column(name = "reviewed_by_mechanic_id")
    private UUID reviewedByMechanicId;

    @Column(name = "mechanic_notes", length = 500)
    private String mechanicNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public AccidentReportJpaEntity() {
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

    public String getVideoObjectKey() {
        return videoObjectKey;
    }

    public void setVideoObjectKey(String videoObjectKey) {
        this.videoObjectKey = videoObjectKey;
    }

    public int getVideoDurationSeconds() {
        return videoDurationSeconds;
    }

    public void setVideoDurationSeconds(int videoDurationSeconds) {
        this.videoDurationSeconds = videoDurationSeconds;
    }

    public long getVideoSizeBytes() {
        return videoSizeBytes;
    }

    public void setVideoSizeBytes(long videoSizeBytes) {
        this.videoSizeBytes = videoSizeBytes;
    }

    public boolean isHasInjuries() {
        return hasInjuries;
    }

    public void setHasInjuries(boolean hasInjuries) {
        this.hasInjuries = hasInjuries;
    }

    public boolean isInvolvesThirdParty() {
        return involvesThirdParty;
    }

    public void setInvolvesThirdParty(boolean involvesThirdParty) {
        this.involvesThirdParty = involvesThirdParty;
    }

    public String getPoliceReportNumber() {
        return policeReportNumber;
    }

    public void setPoliceReportNumber(String policeReportNumber) {
        this.policeReportNumber = policeReportNumber;
    }

    public VehicleDrivability getDrivability() {
        return drivability;
    }

    public void setDrivability(VehicleDrivability drivability) {
        this.drivability = drivability;
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

    public String getRiderToMechanicNotes() {
        return riderToMechanicNotes;
    }

    public void setRiderToMechanicNotes(String riderToMechanicNotes) {
        this.riderToMechanicNotes = riderToMechanicNotes;
    }

    public AccidentReportStatus getStatus() {
        return status;
    }

    public void setStatus(AccidentReportStatus status) {
        this.status = status;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(BigDecimal aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public boolean isStructuralRiskAlert() {
        return structuralRiskAlert;
    }

    public void setStructuralRiskAlert(boolean structuralRiskAlert) {
        this.structuralRiskAlert = structuralRiskAlert;
    }

    public String getAiTechnicalSummary() {
        return aiTechnicalSummary;
    }

    public void setAiTechnicalSummary(String aiTechnicalSummary) {
        this.aiTechnicalSummary = aiTechnicalSummary;
    }

    public BigDecimal getPartsSubtotalBrl() {
        return partsSubtotalBrl;
    }

    public void setPartsSubtotalBrl(BigDecimal partsSubtotalBrl) {
        this.partsSubtotalBrl = partsSubtotalBrl;
    }

    public BigDecimal getLaborFeeBrl() {
        return laborFeeBrl;
    }

    public void setLaborFeeBrl(BigDecimal laborFeeBrl) {
        this.laborFeeBrl = laborFeeBrl;
    }

    public BigDecimal getTowingFeeBrl() {
        return towingFeeBrl;
    }

    public void setTowingFeeBrl(BigDecimal towingFeeBrl) {
        this.towingFeeBrl = towingFeeBrl;
    }

    public BigDecimal getTotalEstimatedBrl() {
        return totalEstimatedBrl;
    }

    public void setTotalEstimatedBrl(BigDecimal totalEstimatedBrl) {
        this.totalEstimatedBrl = totalEstimatedBrl;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}

