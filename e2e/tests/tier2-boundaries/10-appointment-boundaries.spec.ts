import { test, expect } from '../../src/fixtures/test-context.js';
import { AppointmentShift, InspectionStatus } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 2: Boundary 10 - Appointment Date & Shift Boundaries', () => {
  test('TB10.1: EXACT date boundary: scheduledDate = TODAY is ACCEPTED', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/appt-today.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const todayStr = new Date().toISOString().split('T')[0];

    const appt = await riderClient.createAppointment(created.data.inspectionId, {
      yardId: yards.data[0].id,
      scheduledDate: todayStr,
      shift: AppointmentShift.AFTERNOON,
    });

    expect(appt.status).toBe(201);
    expect(appt.data.scheduledDate).toBe(todayStr);
  });

  test('TB10.2: EXACT date boundary: scheduledDate = YESTERDAY is REJECTED with 400 Bad Request', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/appt-yesterday.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const yesterdayStr = new Date(Date.now() - 86400000).toISOString().split('T')[0];

    const appt = await riderClient.createAppointment(created.data.inspectionId, {
      yardId: yards.data[0].id,
      scheduledDate: yesterdayStr,
      shift: AppointmentShift.MORNING,
    });

    expect(appt.status).toBe(400);
    ContractValidator.assertProblemDetail(appt.data, 400);
  });

  test('TB10.3: EXACT date boundary: scheduledDate = TOMORROW is ACCEPTED', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/appt-tomorrow.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const tomorrowStr = new Date(Date.now() + 86400000).toISOString().split('T')[0];

    const appt = await riderClient.createAppointment(created.data.inspectionId, {
      yardId: yards.data[0].id,
      scheduledDate: tomorrowStr,
      shift: AppointmentShift.MORNING,
    });

    expect(appt.status).toBe(201);
    expect(appt.data.scheduledDate).toBe(tomorrowStr);
  });

  test('TB10.4: Appointment shift boundary: supports both MORNING and AFTERNOON enums', async () => {
    expect(Object.values(AppointmentShift)).toContain('MORNING');
    expect(Object.values(AppointmentShift)).toContain('AFTERNOON');
  });

  test('TB10.5: Appointment on inspection transitions status to SCHEDULED', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/appt-status-trans.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const tomorrowStr = new Date(Date.now() + 86400000).toISOString().split('T')[0];

    await riderClient.createAppointment(created.data.inspectionId, {
      yardId: yards.data[0].id,
      scheduledDate: tomorrowStr,
      shift: AppointmentShift.MORNING,
    });

    const detail = await riderClient.getInspection(created.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.SCHEDULED);
    expect(detail.data.appointment).not.toBeNull();
  });
});
