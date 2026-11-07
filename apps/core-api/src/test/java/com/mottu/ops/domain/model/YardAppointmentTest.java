package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.AppointmentShift;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class YardAppointmentTest {

    @Test
    @DisplayName("Should create yard appointment with valid parameters")
    void shouldCreateYardAppointmentSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID inspectionId = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        Instant now = Instant.now();

        YardAppointment appointment = new YardAppointment(
                id,
                inspectionId,
                yardId,
                date,
                AppointmentShift.MORNING,
                now
        );

        assertEquals(id, appointment.getId());
        assertEquals(inspectionId, appointment.getInspectionId());
        assertEquals(yardId, appointment.getYardId());
        assertEquals(date, appointment.getScheduledDate());
        assertEquals(AppointmentShift.MORNING, appointment.getShift());
        assertEquals(now, appointment.getCreatedAt());
    }

    @Test
    @DisplayName("Should auto-generate id and createdAt if null")
    void shouldAutoGenerateIdAndCreatedAtWhenNull() {
        UUID inspectionId = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        YardAppointment appointment = new YardAppointment(
                null,
                inspectionId,
                yardId,
                date,
                AppointmentShift.AFTERNOON,
                null
        );

        assertNotNull(appointment.getId());
        assertEquals(inspectionId, appointment.getInspectionId());
        assertEquals(yardId, appointment.getYardId());
        assertEquals(date, appointment.getScheduledDate());
        assertEquals(AppointmentShift.AFTERNOON, appointment.getShift());
        assertNotNull(appointment.getCreatedAt());
    }

    @Test
    @DisplayName("Should validate required parameters and past dates")
    void shouldValidateConstructorParameters() {
        UUID inspectionId = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        LocalDate futureDate = LocalDate.now().plusDays(2);

        // Null inspectionId
        DomainValidationException ex1 = assertThrows(DomainValidationException.class, () ->
                new YardAppointment(null, null, yardId, futureDate, AppointmentShift.MORNING, null));
        assertEquals("Inspection ID cannot be null", ex1.getMessage());

        // Null yardId
        DomainValidationException ex2 = assertThrows(DomainValidationException.class, () ->
                new YardAppointment(null, inspectionId, null, futureDate, AppointmentShift.MORNING, null));
        assertEquals("Yard ID cannot be null", ex2.getMessage());

        // Null scheduledDate
        DomainValidationException ex3 = assertThrows(DomainValidationException.class, () ->
                new YardAppointment(null, inspectionId, yardId, null, AppointmentShift.MORNING, null));
        assertEquals("Scheduled date cannot be null", ex3.getMessage());

        // Past scheduledDate
        DomainValidationException ex4 = assertThrows(DomainValidationException.class, () ->
                new YardAppointment(null, inspectionId, yardId, LocalDate.now().minusDays(1), AppointmentShift.MORNING, null));
        assertEquals("A data do agendamento não pode ser no passado", ex4.getMessage());

        // Null shift
        DomainValidationException ex5 = assertThrows(DomainValidationException.class, () ->
                new YardAppointment(null, inspectionId, yardId, futureDate, null, null));
        assertEquals("Appointment shift cannot be null", ex5.getMessage());
    }
}
