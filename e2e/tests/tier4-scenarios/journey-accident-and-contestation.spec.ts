import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  DamageVerdict,
  InspectionStatus,
  QueuePriorityReason,
  AppointmentShift,
} from '../../src/contracts/enums.js';

test.describe('Tier 4: Scenario 3 - Accidental Damage & Contestation Journey', () => {
  test('Complete Journey: Bike dropped -> Quoted R$ 60.00 -> Rider contests -> Mechanic reviews & adjusts -> Appointment', async ({
    riderClient,
    mechanicClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    // 1. Rider reports accidental drop
    const upload = await riderClient.getUploadUrl({ contentType: 'image/webp', fileSizeBytes: 400000 });
    const created = await riderClient.createInspection({
      photoObjectKey: upload.data.photoObjectKey,
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1200,
      riderNotes: 'Caiu estacionando no asfalto.',
    });
    expect(created.status).toBe(202);
    const inspId = created.data.inspectionId;

    // 2. Initial automated quote: MAU_USO R$ 60.00
    const initialDetail = await riderClient.getInspection(inspId);
    expect(initialDetail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(initialDetail.data.finalVerdict).toBe(DamageVerdict.MAU_USO);
    expect(initialDetail.data.totalEstimatedBrl).toBe(60.0);
    expect(initialDetail.data.canContest).toBe(true);

    // 3. Rider clicks "Discordo" with justification
    const contest = await riderClient.contestInspection(inspId, {
      reason: 'O impacto não quebrou o manete, apenas ralou a ponta plástica protetora.',
    });
    expect(contest.status).toBe(200);
    expect(contest.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
    expect(contest.data.priorityReason).toBe(QueuePriorityReason.RIDER_CONTESTATION);

    // 4. Mechanic opens queue and inspects case
    const queue = await mechanicClient.getMechanicQueue();
    const item = queue.data.items.find((i) => i.inspectionId === inspId);
    expect(item).toBeDefined();
    expect(item?.priorityWeight).toBe(2);

    // 5. Mechanic checks detail and gives final verdict
    const deskDetail = await mechanicClient.getMechanicInspectionDetail(inspId);
    expect(deskDetail.status).toBe(200);

    const resolve = await mechanicClient.resolveInspection(inspId, {
      finalVerdict: DamageVerdict.MAU_USO,
      catalogPartIds: [],
      mechanicNotes: 'Confirmada avaria por impacto, porém manete funcional. Reposição recomendada.',
    });
    expect(resolve.status).toBe(200);
    expect(resolve.data.status).toBe(InspectionStatus.HUMAN_RESOLVED);
    expect(resolve.data.customerFeedbackText).toBeTruthy();

    // 6. Rider cannot contest again
    const finalDetail = await riderClient.getInspection(inspId);
    expect(finalDetail.data.contestedOnce).toBe(true);
    expect(finalDetail.data.canContest).toBe(false);

    // 7. Rider schedules repair at yard
    const yards = await riderClient.getYards();
    const appt = await riderClient.createAppointment(inspId, {
      yardId: yards.data[0].id,
      scheduledDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      shift: AppointmentShift.MORNING,
    });
    expect(appt.status).toBe(201);
  });
});
