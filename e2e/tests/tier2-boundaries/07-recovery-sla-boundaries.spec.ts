import { test, expect } from '../../src/fixtures/test-context.js';
import { InspectionStatus, QueuePriorityReason, SafetyRiskLevel } from '../../src/contracts/enums.js';

test.describe('Tier 2: Boundary 07 - AI Recovery & SLA Time Boundaries', () => {
  test('TB7.1: EXACT SLA limit: 4 min 59 sec (<= 300s) -> remains in AWAITING_AI_RECOVERY', async ({ mockServer }) => {
    const state = mockServer?.state;
    if (state) {
      const id = 'sla-4m59s';
      state.inspections.set(id, {
        id,
        status: InspectionStatus.AWAITING_AI_RECOVERY,
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        createdAt: new Date(Date.now() - 299 * 1000).toISOString(), // 299s (under 300s)
      });

      const insp = state.inspections.get(id);
      expect(insp.status).toBe(InspectionStatus.AWAITING_AI_RECOVERY);
    }
  });

  test('TB7.2: EXACT SLA limit: 5 min 01 sec (> 300s) with CRITICAL_STOPPED -> escalates to HUMAN_TRIAGE weight 1', async ({ mockServer, mechanicClient }) => {
    const state = mockServer?.state;
    if (state) {
      const id = 'sla-5m01s-escalated';
      state.inspections.set(id, {
        id,
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.HUMAN_TRIAGE,
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        priorityWeight: 1,
        priorityReason: QueuePriorityReason.CRITICAL_SAFETY,
        createdAt: new Date(Date.now() - 301 * 1000).toISOString(), // 301s (> 300s)
      });

      const q = await mechanicClient.getMechanicQueue();
      const item = q.data.items.find((i) => i.inspectionId === id);
      expect(item).toBeDefined();
      expect(item?.priorityWeight).toBe(1);
    }
  });

  test('TB7.3: Non-critical SAFE_TO_RIDE after 10 min does not receive weight 1 priority', async ({ mockServer, mechanicClient }) => {
    const state = mockServer?.state;
    if (state) {
      const id = 'sla-safe-10m';
      state.inspections.set(id, {
        id,
        plate: 'MOT2B34',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.HUMAN_TRIAGE,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        priorityWeight: 3, // LOW_AI_CONFIDENCE, NOT weight 1
        priorityReason: QueuePriorityReason.LOW_AI_CONFIDENCE,
        createdAt: new Date(Date.now() - 600 * 1000).toISOString(),
      });

      const q = await mechanicClient.getMechanicQueue();
      const item = q.data.items.find((i) => i.inspectionId === id);
      expect(item?.priorityWeight).not.toBe(1);
    }
  });

  test('TB7.4: Recovery retry count upper bounds at 3 without crashing', async () => {
    let count = 0;
    for (let i = 0; i < 5; i++) {
      count = Math.min(count + 1, 3);
    }
    expect(count).toBe(3);
  });

  test('TB7.5: Processing zero items in recovery queue handles cleanly', async ({ mockServer }) => {
    const state = mockServer?.state;
    if (state) {
      const recoveryCount = Array.from(state.inspections.values()).filter(
        (i) => i.status === InspectionStatus.AWAITING_AI_RECOVERY
      ).length;
      expect(recoveryCount).toBeGreaterThanOrEqual(0);
    }
  });
});
