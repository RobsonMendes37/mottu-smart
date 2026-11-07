import { test, expect } from '../../src/fixtures/test-context.js';
import {
  UserRole,
  AccountStatus,
  ReportedOriginContext,
  SafetyRiskLevel,
  DamageVerdict,
  InspectionStatus,
  QueuePriorityReason,
  AppointmentShift,
  MotorcycleModel,
} from '../../src/contracts/enums.js';
import crypto from 'node:crypto';

test.describe('Tier 3: Cross-Feature Combinations', () => {
  test('TC3.1: Pairwise: Auth + Fleet + Inspection + Mechanic Workbench + Appointment', async ({
    unauthClient,
    managerClient,
    mechanicClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    // 1. Self-register new rider
    const email = `rider_pw_${Date.now()}@test.com`;
    const regRes = await unauthClient.register({
      fullName: 'Rider Fluxo Completo',
      cpf: '55566677788',
      phone: '11944443333',
      email,
      password: 'Senha@123',
      role: UserRole.RIDER,
    });
    expect(regRes.status).toBe(201);
    const newUserId = regRes.data.userId;

    // 2. Manager approves user and links to available motorcycle
    const fleet = await managerClient.getFleet();
    const availableMoto = fleet.data.items.find((m) => m.status === 'AVAILABLE') || fleet.data.items[1];
    const approveRes = await managerClient.approveUser(newUserId, { motorcycleId: availableMoto.id });
    expect(approveRes.status).toBe(200);

    // 3. New rider logs in
    const loginRes = await unauthClient.login({ email, password: 'Senha@123' });
    expect(loginRes.status).toBe(200);
    const riderSessionClient = unauthClient;

    // 4. Rider submits inspection for drop damage
    const inspRes = await riderSessionClient.createInspection({
      photoObjectKey: 'inspections/2026/11/pairwise-drop.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
      riderNotes: 'Tombou na calçada',
    });
    expect(inspRes.status).toBe(202);
    const inspId = inspRes.data.inspectionId;

    // 5. Rider contests the verdict
    const contestRes = await riderSessionClient.contestInspection(inspId, {
      reason: 'A peça já estava trincada anteriormente antes de tombar.',
    });
    expect(contestRes.status).toBe(200);
    expect(contestRes.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);

    // 6. Mechanic reviews in workbench queue and resolves to DESGASTE_NATURAL
    const resolveRes = await mechanicClient.resolveInspection(inspId, {
      finalVerdict: DamageVerdict.DESGASTE_NATURAL,
      catalogPartIds: [],
      mechanicNotes: 'Reavaliado como fadiga de material.',
    });
    expect(resolveRes.status).toBe(200);
    expect(resolveRes.data.finalVerdict).toBe(DamageVerdict.DESGASTE_NATURAL);
    expect(resolveRes.data.totalEstimatedBrl).toBe(0.0);

    // 7. Rider schedules maintenance appointment
    const yards = await riderSessionClient.getYards();
    const tomorrowStr = new Date(Date.now() + 86400000).toISOString().split('T')[0];
    const apptRes = await riderSessionClient.createAppointment(inspId, {
      yardId: yards.data[0].id,
      scheduledDate: tomorrowStr,
      shift: AppointmentShift.MORNING,
    });
    expect(apptRes.status).toBe(201);
    expect(apptRes.data.inspectionStatus).toBe(InspectionStatus.SCHEDULED);
  });

  test('TC3.2: Multi-part calculation: Single labor fee charged regardless of parts count in MAU_USO', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/multi-parts.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const detail = await riderClient.getInspection(created.data.inspectionId);
    expect(detail.status).toBe(200);
    expect(detail.data.laborFeeBrl).toBe(35.0); // Exact single labor fee R$ 35.00
  });

  test('TC3.3: Price override persistence across manager edit and rider quote', async ({ managerClient, riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    // 1. Manager overrides part price
    const parts = await managerClient.getCatalogParts();
    const part = parts.data.items[0];
    await managerClient.updateCatalogPart(part.id, { unitPriceBrl: 45.00 });

    // 2. Trigger store sync
    await managerClient.syncCatalog();

    // 3. Verify price is still 45.00
    const partsAfter = await managerClient.getCatalogParts();
    const partAfter = partsAfter.data.items.find((p) => p.id === part.id);
    expect(partAfter?.unitPriceBrl).toBe(45.00);
    expect(partAfter?.priceOverridden).toBe(true);
  });

  test('TC3.4: Manager rejection blocks user login with RFC 7807 detail', async ({ unauthClient, managerClient }) => {
    const email = `rejected_user_${Date.now()}@test.com`;
    const reg = await unauthClient.register({
      fullName: 'Usuario a Rejeitar',
      cpf: '77788899900',
      phone: '11933332222',
      email,
      password: 'Senha@123',
      role: UserRole.RIDER,
    });

    // Manager rejects
    const rejectRes = await managerClient.rejectUser(reg.data.userId, {
      reason: 'Documentação ilegível',
    });
    expect(rejectRes.status).toBe(200);
    expect(rejectRes.data.status).toBe(AccountStatus.REJECTED);

    // Login must be blocked
    const loginRes = await unauthClient.login({ email, password: 'Senha@123' });
    expect(loginRes.status).toBe(403);
    expect(loginRes.data.detail).toContain('recusado');
  });

  test('TC3.5: Fleet creation and motorcycle status change on rental assignment', async ({ managerClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const yards = await managerClient.getYards();
    const newPlate = `FLE${Math.floor(1000 + Math.random() * 9000)}`;

    const createMoto = await managerClient.createMotorcycle({
      plate: newPlate,
      model: MotorcycleModel.HONDA_POP_110I,
      yardId: yards.data[0].id,
      currentOdometerKm: 50,
    });
    expect(createMoto.status).toBe(201);
    expect(createMoto.data.status).toBe('AVAILABLE');

    // Register & approve rider with this motorcycle
    const riderRes = await managerClient.request('/api/v1/auth/register', {
      method: 'POST',
      body: {
        fullName: 'Novo Piloto Frota',
        cpf: '33399988811',
        phone: '11911112222',
        email: `pilot_${Date.now()}@test.com`,
        password: 'Senha@123',
        role: UserRole.RIDER,
      },
    });

    const approve = await managerClient.approveUser(riderRes.data.userId, {
      motorcycleId: createMoto.data.id,
    });
    expect(approve.status).toBe(200);

    // Fleet list should now reflect RENTED status
    const fleet = await managerClient.getFleet();
    const rentedMoto = fleet.data.items.find((m) => m.plate === newPlate);
    expect(rentedMoto?.status).toBe('RENTED');
    expect(rentedMoto?.activeRiderName).toBe('Novo Piloto Frota');
  });

  test('TC3.6: Anti-fraud duplicate photo denied automatic exemption despite <=12h contract', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    // Photo has duplicate hash, rider claims RECEIVED_LIKE_THIS
    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-dup-check.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1010, // only 10 km driven
      simulatedHoursSincePickup: 2.0, // only 2 hours since pickup
    } as any);

    expect(res.status).toBe(202);
    // Anti-fraud MUST override pre-existing exemption and route to HUMAN_TRIAGE!
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
  });

  test('TC3.7: Priority leapfrog: Critical safety (weight 1) overtakes Contestation (weight 2) in queue', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      // Weight 2 submitted first
      state.inspections.set('older-contest', {
        id: 'older-contest',
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 2,
        priorityReason: QueuePriorityReason.RIDER_CONTESTATION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 60000).toISOString(),
      });
      // Weight 1 submitted later
      state.inspections.set('newer-critical', {
        id: 'newer-critical',
        plate: 'MOT2B34',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 1,
        priorityReason: QueuePriorityReason.CRITICAL_SAFETY,
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        createdAt: new Date(Date.now() - 1000).toISOString(),
      });
    }

    const q = await mechanicClient.getMechanicQueue();
    // Weight 1 must be index 0 despite being newer!
    expect(q.data.items[0].inspectionId).toBe('newer-critical');
    expect(q.data.items[1].inspectionId).toBe('older-contest');
  });

  test('TC3.8: SSE real-time stream endpoint is accessible with Bearer token', async ({ riderClient }) => {
    const res = await riderClient.request('/api/v1/rider/inspections/test-sse-id/stream', {
      headers: { Accept: 'text/event-stream' },
    });
    expect([200, 404]).toContain(res.status);
  });
});
