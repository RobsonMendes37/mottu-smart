package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalContractTest {

    @Test
    @DisplayName("Should create active contract using factory method")
    void shouldCreateContract() {
        UUID riderId = UUID.randomUUID();
        UUID motorcycleId = UUID.randomUUID();
        Instant pickup = Instant.now();

        RentalContract contract = RentalContract.create(riderId, motorcycleId, pickup, 1000);

        assertNotNull(contract.getId());
        assertEquals(riderId, contract.getRiderId());
        assertEquals(motorcycleId, contract.getMotorcycleId());
        assertEquals(pickup, contract.getPickedUpAt());
        assertEquals(1000, contract.getInitialOdometerKm());
        assertNull(contract.getEndedAt());
        assertTrue(contract.isActive());

        RentalContract contractWithGeneratedId = new RentalContract(null, riderId, motorcycleId, pickup, 1000, null, true);
        assertNotNull(contractWithGeneratedId.getId());
    }

    @Test
    @DisplayName("Should compute duration and hours since pickup correctly")
    void shouldComputeDurationAndHours() {
        Instant pickup = Instant.parse("2026-11-01T10:00:00Z");
        RentalContract contract = RentalContract.create(UUID.randomUUID(), UUID.randomUUID(), pickup, 1000);

        Instant evaluationTime = pickup.plus(12, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);
        Duration duration = contract.durationSincePickup(evaluationTime);
        double hours = contract.hoursSincePickup(evaluationTime);

        assertEquals(12 * 3600 + 30 * 60, duration.toSeconds());
        assertEquals(12.5, hours, 0.001);

        assertThrows(DomainValidationException.class, () ->
                contract.durationSincePickup(pickup.minus(1, ChronoUnit.MINUTES))
        );
    }

    @Test
    @DisplayName("Should compute distance driven in km correctly")
    void shouldComputeDistanceDriven() {
        RentalContract contract = RentalContract.create(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), 1000);

        int distance = contract.distanceDrivenKm(1150);
        assertEquals(150, distance);

        assertThrows(DomainValidationException.class, () -> contract.distanceDrivenKm(950));
    }

    @Test
    @DisplayName("Should close active contract successfully")
    void shouldCloseContract() {
        Instant pickup = Instant.now();
        RentalContract contract = RentalContract.create(UUID.randomUUID(), UUID.randomUUID(), pickup, 1000);

        Instant returnTime = pickup.plus(2, ChronoUnit.DAYS);
        contract.close(returnTime, 1250);

        assertFalse(contract.isActive());
        assertEquals(returnTime, contract.getEndedAt());

        // Attempting to close again throws BusinessRuleViolationException
        assertThrows(BusinessRuleViolationException.class, () -> contract.close(returnTime.plusSeconds(60), 1260));
    }

    @Test
    @DisplayName("close throws when return time is before pickup or odometer is less than initial")
    void shouldThrowWhenCloseParametersInvalid() {
        Instant pickup = Instant.now();
        RentalContract contract = RentalContract.create(UUID.randomUUID(), UUID.randomUUID(), pickup, 1000);

        assertThrows(DomainValidationException.class, () ->
                contract.close(pickup.minusSeconds(100), 1100));

        assertThrows(DomainValidationException.class, () ->
                contract.close(pickup.plusSeconds(100), 900));
    }

    @Test
    @DisplayName("Constructor parameter validations")
    void shouldValidateConstructor() {
        UUID validId = UUID.randomUUID();
        Instant validTime = Instant.now();

        assertThrows(DomainValidationException.class, () ->
                new RentalContract(validId, null, validId, validTime, 1000, null, true));
        assertThrows(DomainValidationException.class, () ->
                new RentalContract(validId, validId, null, validTime, 1000, null, true));
        assertThrows(DomainValidationException.class, () ->
                new RentalContract(validId, validId, validId, null, 1000, null, true));
        assertThrows(DomainValidationException.class, () ->
                new RentalContract(validId, validId, validId, validTime, -1, null, true));
    }
}
