package com.mottu.ops.domain.model;

import com.mottu.ops.domain.service.PreExistingRuleCalculator;

import java.time.Duration;
import java.time.Instant;

public record BaselineShieldStatus(
        boolean isShieldActive,
        double remainingHours,
        int remainingKm,
        double elapsedHours,
        int elapsedKm
) {
    public static BaselineShieldStatus calculate(Instant pickedUpAt, int initialOdometerKm, int currentOdometerKm, Instant evaluationTime) {
        if (pickedUpAt == null) {
            return new BaselineShieldStatus(false, 0.0, 0, 0.0, 0);
        }

        Instant now = evaluationTime != null ? evaluationTime : Instant.now();
        double elapsedHours = Math.max(0.0, Duration.between(pickedUpAt, now).toMinutes() / 60.0);
        int elapsedKm = Math.max(0, currentOdometerKm - initialOdometerKm);

        double remainingHours = Math.max(0.0, PreExistingRuleCalculator.MAX_HOURS - elapsedHours);
        int remainingKm = Math.max(0, PreExistingRuleCalculator.MAX_KM - elapsedKm);

        boolean active = (elapsedHours <= PreExistingRuleCalculator.MAX_HOURS && elapsedKm <= PreExistingRuleCalculator.MAX_KM);

        return new BaselineShieldStatus(
                active,
                remainingHours,
                remainingKm,
                elapsedHours,
                elapsedKm
        );
    }
}
