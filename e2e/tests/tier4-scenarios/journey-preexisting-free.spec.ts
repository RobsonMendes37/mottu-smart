import { test, expect } from '../../src/fixtures/test-context.js';
import {
  UserRole,
  ReportedOriginContext,
  SafetyRiskLevel,
  DamageVerdict,
  InspectionStatus,
  AppointmentShift,
} from '../../src/contracts/enums.js';

test.describe('Tier 4: Scenario 1 - Pre-Existing Damage Autonomous Resolution Journey', () => {
  test('Complete Journey: New Rider receives bike -> Reports scratch in 12h -> Auto-approved R$ 0.00 -> Schedules maintenance', async ({
    unauthClient,
    managerClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    // 1. Rider signs up
    const email = `rider_pre_${Date.now()}@test.com`;
    const regRes = await unauthClient.register({
      fullName: 'João Silva Entregador',
      cpf: '33344455566',
      phone: '11988881111',
      email,
      password: 'Senha@123',
      role: UserRole.RIDER,
    });
    expect(regRes.status).toBe(201);
    const riderId = regRes.data.userId;

    // 2. Manager approves and binds bike
    const fleet = await managerClient.getFleet();
    const bike = fleet.data.items[1];
    await managerClient.approveUser(riderId, { motorcycleId: bike.id });

    // 3. Rider logs in and checks garage
    await unauthClient.login({ email, password: 'Senha@123' });
    const garage = await unauthClient.getGarage();
    expect(garage.status).toBe(200);
    expect(garage.data.activeContract).not.toBeNull();
    expect(garage.data.activeContract?.motorcycle.plate).toBe(bike.plate);

    // 4. Rider notices pre-existing scratch and requests S3 upload URL
    const uploadUrlRes = await unauthClient.getUploadUrl({
      contentType: 'image/webp',
      fileSizeBytes: 420000,
    });
    expect(uploadUrlRes.status).toBe(200);
    const photoKey = uploadUrlRes.data.photoObjectKey;

    // 5. Rider submits inspection with context RECEIVED_LIKE_THIS within 12 hours
    const inspRes = await unauthClient.createInspection({
      photoObjectKey: photoKey,
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: garage.data.activeContract!.initialOdometerKm + 25, // only 25 km driven
      riderNotes: 'Peguei a moto ontem à tarde e vi a carenagem trincada.',
      simulatedHoursSincePickup: 12.0,
    } as any);
    expect(inspRes.status).toBe(202);
    const inspId = inspRes.data.inspectionId;

    // 6. AI evaluates: <= 48h, <= 100km, confidence >= 0.80 -> AUTO_RESOLVED with R$ 0.00
    const detail = await unauthClient.getInspection(inspId);
    expect(detail.status).toBe(200);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.PRE_EXISTENTE);
    expect(detail.data.totalEstimatedBrl).toBe(0.0);
    expect(detail.data.laborFeeBrl).toBe(0.0);

    // 7. Rider schedules maintenance at operational yard
    const yards = await unauthClient.getYards();
    const nextWeek = new Date(Date.now() + 5 * 86400000).toISOString().split('T')[0];
    const apptRes = await unauthClient.createAppointment(inspId, {
      yardId: yards.data[0].id,
      scheduledDate: nextWeek,
      shift: AppointmentShift.MORNING,
    });
    expect(apptRes.status).toBe(201);
    expect(apptRes.data.inspectionStatus).toBe(InspectionStatus.SCHEDULED);

    // 8. Final check: Garage shows scheduled status in recent list
    const finalGarage = await unauthClient.getGarage();
    expect(finalGarage.status).toBe(200);
    const recent = finalGarage.data.recentInspections.find((i) => i.id === inspId);
    expect(recent?.status).toBe(InspectionStatus.SCHEDULED);
  });
});
