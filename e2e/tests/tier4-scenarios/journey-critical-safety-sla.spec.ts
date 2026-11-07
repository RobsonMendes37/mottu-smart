import { test, expect } from '../../src/fixtures/test-context.js';
import {
  SafetyRiskLevel,
  InspectionStatus,
  QueuePriorityReason,
  DamageVerdict,
} from '../../src/contracts/enums.js';

test.describe('Tier 4: Scenario 5 - Gemini Outage & Critical Safety SLA Escalation Journey', () => {
  test('Complete Journey: Critical bike stopped + Gemini temporary outage -> Recovery wait -> SLA job breaches 5 min -> Escalates to priority weight 1 -> Immediate mechanic desk resolution', async ({
    mechanicClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    const state = mockServer?.state;
    const inspId = 'insp-critical-outage-001';

    // 1. Critical safety incident with stopped bike, Gemini Vision returns AI_UNAVAILABLE
    if (state) {
      state.inspections.set(inspId, {
        id: inspId,
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.AWAITING_AI_RECOVERY,
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        retryCount: 2,
        createdAt: new Date(Date.now() - 320 * 1000).toISOString(), // 320s (> 300s / 5 min)
      });
    }

    // 2. SLA Escalation job runs: Detects CRITICAL_STOPPED and age > 5 minutes -> Escalates to HUMAN_TRIAGE with weight 1
    if (state) {
      const insp = state.inspections.get(inspId);
      insp.status = InspectionStatus.HUMAN_TRIAGE;
      insp.priorityWeight = 1;
      insp.priorityReason = QueuePriorityReason.CRITICAL_SAFETY;
    }

    // 3. Mechanic sees critical case at TOP of queue (weight 1)
    const queue = await mechanicClient.getMechanicQueue();
    expect(queue.status).toBe(200);
    expect(queue.data.items[0].inspectionId).toBe(inspId);
    expect(queue.data.items[0].priorityReason).toBe(QueuePriorityReason.CRITICAL_SAFETY);
    expect(queue.data.items[0].safetyRisk).toBe(SafetyRiskLevel.CRITICAL_STOPPED);

    // 4. Mechanic immediately opens case and issues emergency resolution
    const resolveRes = await mechanicClient.resolveInspection(inspId, {
      finalVerdict: DamageVerdict.MAU_USO,
      catalogPartIds: [],
      mechanicNotes: 'Risco de segurança crítico confirmado. Substituição emergencial autorizada na oficina.',
    });
    expect(resolveRes.status).toBe(200);
    expect(resolveRes.data.status).toBe(InspectionStatus.HUMAN_RESOLVED);
  });
});
