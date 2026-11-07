import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  InspectionStatus,
  QueuePriorityReason,
  DamageVerdict,
} from '../../src/contracts/enums.js';

test.describe('Tier 4: Scenario 4 - Photo Fraud & Perceptual Hash Quarantine Journey', () => {
  test('Complete Journey: Malicious user submits duplicate photo -> Caught by SHA256/dHash -> Quarantined in mechanic queue weight 4 -> Denied auto-exemption', async ({
    riderClient,
    mechanicClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    // 1. Rider attempts to reuse photo (flagged by backend S3 SHA-256 reader)
    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-reused-sha256.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS, // claims pre-existing to get free exemption
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1010, // within 10 km
      simulatedHoursSincePickup: 1.0, // within 1 hour
    } as any);

    expect(res.status).toBe(202);
    const inspId = res.data.inspectionId;

    // 2. Inspection is quarantined: status = HUMAN_TRIAGE, fraud_suspected = true, priorityReason = FRAUD_SUSPICION (weight 4)
    const detail = await riderClient.getInspection(inspId);
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);

    // 3. Mechanic sees item in queue under priority reason FRAUD_SUSPICION
    const queue = await mechanicClient.getMechanicQueue();
    const item = queue.data.items.find((i) => i.inspectionId === inspId);
    expect(item).toBeDefined();
    expect(item?.priorityReason).toBe(QueuePriorityReason.FRAUD_SUSPICION);
    expect(item?.priorityWeight).toBe(4);
    expect(item?.fraudSuspected).toBe(true);

    // 4. Mechanic opens decision desk and reviews fraud alert
    const desk = await mechanicClient.getMechanicInspectionDetail(inspId);
    expect(desk.data.fraudSuspected).toBe(true);

    // 5. Mechanic rejects fraudulent free claim and applies MAU_USO verdict
    const resolved = await mechanicClient.resolveInspection(inspId, {
      finalVerdict: DamageVerdict.MAU_USO,
      catalogPartIds: [],
      mechanicNotes: 'Foto idêntica a sinistro anterior detectada pelo sistema anti-fraude. Cobrança mantida.',
    });
    expect(resolved.status).toBe(200);
    expect(resolved.data.finalVerdict).toBe(DamageVerdict.MAU_USO);
  });
});
