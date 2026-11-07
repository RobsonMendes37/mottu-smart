import { test, expect } from '../../src/fixtures/test-context.js';
import { AppointmentShift, InspectionStatus } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 1: Feature 10 - Yard Appointment Scheduling', () => {
  test('TC10.1: Schedule appointment for future date and MORNING shift returns 201 Created and status SCHEDULED', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-sched.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const yardId = yards.data[0].id;

    const tomorrow = new Date(Date.now() + 24 * 3600 * 1000).toISOString().split('T')[0];
    const apptRes = await riderClient.createAppointment(created.data.inspectionId, {
      yardId,
      scheduledDate: tomorrow,
      shift: AppointmentShift.MORNING,
    });

    expect(apptRes.status).toBe(201);
    expect(ContractValidator.isUUID(apptRes.data.appointmentId)).toBe(true);
    expect(apptRes.data.inspectionStatus).toBe(InspectionStatus.SCHEDULED);
    expect(apptRes.data.shift).toBe(AppointmentShift.MORNING);
    expect(apptRes.data.scheduledDate).toBe(tomorrow);
  });

  test('TC10.2: Schedule appointment for AFTERNOON shift returns 201 Created', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-afternoon.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const yardId = yards.data[0].id;
    const futureDate = new Date(Date.now() + 48 * 3600 * 1000).toISOString().split('T')[0];

    const apptRes = await riderClient.createAppointment(created.data.inspectionId, {
      yardId,
      scheduledDate: futureDate,
      shift: AppointmentShift.AFTERNOON,
    });

    expect(apptRes.status).toBe(201);
    expect(apptRes.data.shift).toBe(AppointmentShift.AFTERNOON);
  });

  test('TC10.3: Scheduling appointment for past date returns 400 Bad Request', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/past-date.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const yardId = yards.data[0].id;

    const yesterday = new Date(Date.now() - 24 * 3600 * 1000).toISOString().split('T')[0];
    const apptRes = await riderClient.createAppointment(created.data.inspectionId, {
      yardId,
      scheduledDate: yesterday,
      shift: AppointmentShift.MORNING,
    });

    expect(apptRes.status).toBe(400);
    ContractValidator.assertProblemDetail(apptRes.data, 400);
    expect(apptRes.data.detail).toContain('passado');
  });

  test('TC10.4: Appointment associates active operational yard with valid yardName', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/yard-assoc.webp',
      reportedContext: 'ACCIDENT_OR_DROP' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1050,
    });

    const yards = await riderClient.getYards();
    const targetYard = yards.data[1]; // Guarulhos
    const futureDate = new Date(Date.now() + 72 * 3600 * 1000).toISOString().split('T')[0];

    const apptRes = await riderClient.createAppointment(created.data.inspectionId, {
      yardId: targetYard.id,
      scheduledDate: futureDate,
      shift: AppointmentShift.MORNING,
    });

    expect(apptRes.status).toBe(201);
    expect(apptRes.data.yardName).toBe(targetYard.name);
  });

  test('TC10.5: Scheduling appointment for non-existent inspection returns 404 Not Found', async ({ riderClient }) => {
    const fakeId = '00000000-0000-0000-0000-999999999999';
    const tomorrow = new Date(Date.now() + 24 * 3600 * 1000).toISOString().split('T')[0];

    const apptRes = await riderClient.createAppointment(fakeId, {
      yardId: '11111111-1111-1111-1111-111111111111',
      scheduledDate: tomorrow,
      shift: AppointmentShift.MORNING,
    });

    expect(apptRes.status).toBe(404);
    ContractValidator.assertProblemDetail(apptRes.data, 404);
  });
});
