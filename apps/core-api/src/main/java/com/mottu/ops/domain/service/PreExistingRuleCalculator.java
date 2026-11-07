package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.model.PreExistingDamageRecord;
import com.mottu.ops.domain.model.PreExistingEvaluationResult;
import com.mottu.ops.domain.model.RentalContract;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class PreExistingRuleCalculator {

    public static final double MAX_HOURS = 48.0;
    public static final int MAX_KM = 100;

    public boolean isEligibleByWindow(
            RentalContract contract,
            int reportedOdometerKm,
            Instant inspectionTimestamp,
            ReportedOriginContext reportedContext,
            boolean fraudSuspected
    ) {
        Objects.requireNonNull(contract, "Rental contract cannot be null");
        Objects.requireNonNull(inspectionTimestamp, "Inspection timestamp cannot be null");
        Objects.requireNonNull(reportedContext, "Reported context cannot be null");

        if (fraudSuspected) {
            return false;
        }

        if (reportedContext != ReportedOriginContext.RECEIVED_LIKE_THIS) {
            return false;
        }

        if (inspectionTimestamp.isBefore(contract.getPickedUpAt())) {
            throw new DomainValidationException("Inspection timestamp cannot be before contract pickup timestamp");
        }

        if (reportedOdometerKm < contract.getInitialOdometerKm()) {
            throw new DomainValidationException(
                    "Reported odometer (" + reportedOdometerKm + " km) cannot be less than initial contract odometer (" + contract.getInitialOdometerKm() + " km)"
            );
        }

        Duration duration = Duration.between(contract.getPickedUpAt(), inspectionTimestamp);
        double hoursElapsed = duration.toSeconds() / 3600.0;
        int kmDriven = reportedOdometerKm - contract.getInitialOdometerKm();

        // 48h OR 100km rule (per docs/03-CASOS-DE-USO-TDD.md UC-05)
        return (hoursElapsed <= MAX_HOURS) || (kmDriven <= MAX_KM);
    }

    public PreExistingEvaluationResult evaluate(
            RentalContract contract,
            int reportedOdometerKm,
            Instant inspectionTimestamp,
            ReportedOriginContext reportedContext,
            boolean fraudSuspected,
            List<PreExistingDamageRecord> baselineDamages,
            String reportedPartName
    ) {
        if (fraudSuspected) {
            return PreExistingEvaluationResult.notExempt("Inspection flagged for suspected fraud");
        }

        // Check if damage matches known baseline damage records pre-recorded at pickup
        if (baselineDamages != null && reportedPartName != null) {
            for (PreExistingDamageRecord record : baselineDamages) {
                if (record.matchesPart(reportedPartName)) {
                    return PreExistingEvaluationResult.exempt(
                            "Damage matches pre-existing baseline handover record: " + record.partName()
                    );
                }
            }
        }

        // Evaluate handover window (48h / 100km)
        boolean eligibleByWindow = isEligibleByWindow(
                contract,
                reportedOdometerKm,
                inspectionTimestamp,
                reportedContext,
                false
        );

        if (eligibleByWindow) {
            return PreExistingEvaluationResult.exempt(
                    "Damage reported within handover window (<= 48.0h or <= 100km) with context RECEIVED_LIKE_THIS"
            );
        }

        return PreExistingEvaluationResult.notExempt(
                "Handover window exceeded (> 48.0h and > 100km) and no matching baseline damage record found"
        );
    }
}
