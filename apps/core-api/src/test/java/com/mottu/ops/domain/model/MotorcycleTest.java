package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotorcycleTest {

    @Test
    @DisplayName("Should create motorcycle with valid fields and getters")
    void shouldCreateMotorcycle() {
        UUID id = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        Instant now = Instant.now();

        Motorcycle moto = new Motorcycle(
                id,
                "mot1234",
                MotorcycleModel.MOTTU_SPORT_110I,
                yardId,
                MotorcycleStatus.AVAILABLE,
                5000,
                now
        );

        assertEquals(id, moto.getId());
        assertEquals("MOT1234", moto.getPlate());
        assertEquals(MotorcycleModel.MOTTU_SPORT_110I, moto.getModel());
        assertEquals(yardId, moto.getYardId());
        assertEquals(MotorcycleStatus.AVAILABLE, moto.getStatus());
        assertEquals(5000, moto.getCurrentOdometerKm());
        assertEquals(now, moto.getCreatedAt());
    }

    @Test
    @DisplayName("updateOdometer updates mileage when greater or equal")
    void shouldUpdateOdometer() {
        Motorcycle moto = new Motorcycle(
                null,
                "ABC1234",
                MotorcycleModel.MOTTU_SPORT_110I,
                UUID.randomUUID(),
                MotorcycleStatus.AVAILABLE,
                5000,
                null
        );

        moto.updateOdometer(5200);
        assertEquals(5200, moto.getCurrentOdometerKm());

        assertThrows(DomainValidationException.class, () -> moto.updateOdometer(5100));
    }

    @Test
    @DisplayName("rent changes status from AVAILABLE to RENTED")
    void shouldRentMotorcycle() {
        Motorcycle moto = new Motorcycle(
                null,
                "ABC1234",
                MotorcycleModel.MOTTU_SPORT_110I,
                UUID.randomUUID(),
                MotorcycleStatus.AVAILABLE,
                1000,
                null
        );

        moto.rent();
        assertEquals(MotorcycleStatus.RENTED, moto.getStatus());

        assertThrows(BusinessRuleViolationException.class, moto::rent);
    }

    @Test
    @DisplayName("returnToYard updates yard, odometer, and sets status to AVAILABLE")
    void shouldReturnToYard() {
        Motorcycle moto = new Motorcycle(
                null,
                "ABC1234",
                MotorcycleModel.MOTTU_SPORT_110I,
                UUID.randomUUID(),
                MotorcycleStatus.AVAILABLE,
                1000,
                null
        );
        moto.rent();

        UUID newYard = UUID.randomUUID();
        moto.returnToYard(newYard, 1200);

        assertEquals(newYard, moto.getYardId());
        assertEquals(1200, moto.getCurrentOdometerKm());
        assertEquals(MotorcycleStatus.AVAILABLE, moto.getStatus());

        assertThrows(BusinessRuleViolationException.class, () -> moto.returnToYard(UUID.randomUUID(), 1300));
    }

    @Test
    @DisplayName("sendToMaintenance and releaseFromMaintenance transition status correctly")
    void shouldHandleMaintenanceLifecycle() {
        Motorcycle moto = new Motorcycle(
                null,
                "ABC1234",
                MotorcycleModel.MOTTU_SPORT_110I,
                UUID.randomUUID(),
                MotorcycleStatus.AVAILABLE,
                1000,
                null
        );

        moto.sendToMaintenance();
        assertEquals(MotorcycleStatus.IN_MAINTENANCE, moto.getStatus());

        moto.releaseFromMaintenance();
        assertEquals(MotorcycleStatus.AVAILABLE, moto.getStatus());
    }

    @Test
    @DisplayName("Constructor parameter validations")
    void shouldValidateConstructor() {
        UUID validYard = UUID.randomUUID();

        // null or blank plate
        assertThrows(DomainValidationException.class, () ->
                new Motorcycle(null, null, MotorcycleModel.MOTTU_SPORT_110I, validYard, MotorcycleStatus.AVAILABLE, 0, null));
        assertThrows(DomainValidationException.class, () ->
                new Motorcycle(null, "   ", MotorcycleModel.MOTTU_SPORT_110I, validYard, MotorcycleStatus.AVAILABLE, 0, null));

        // null model, yard, status, negative odometer
        assertThrows(DomainValidationException.class, () ->
                new Motorcycle(null, "ABC1234", null, validYard, MotorcycleStatus.AVAILABLE, 0, null));
        assertThrows(DomainValidationException.class, () ->
                new Motorcycle(null, "ABC1234", MotorcycleModel.MOTTU_SPORT_110I, null, MotorcycleStatus.AVAILABLE, 0, null));
        assertThrows(DomainValidationException.class, () ->
                new Motorcycle(null, "ABC1234", MotorcycleModel.MOTTU_SPORT_110I, validYard, null, 0, null));
        assertThrows(DomainValidationException.class, () ->
                new Motorcycle(null, "ABC1234", MotorcycleModel.MOTTU_SPORT_110I, validYard, MotorcycleStatus.AVAILABLE, -1, null));
    }
}
