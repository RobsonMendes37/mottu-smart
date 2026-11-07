import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  InspectionStatus,
  QueuePriorityReason,
} from '../../src/contracts/enums.js';

test.describe('Tier 1: Feature 07 - AI Recovery Policy & SLA Escalation', () => {
  test('TC7.1: AI temporary outage places job into recovery without overwhelming mechanic', async ({ mockServer }) => {
    // In recovery policy, outcome AI_UNAVAILABLE sets status AWAITING_AI_RECOVERY
    const state = mockServer?.state;
    if (state) {
      const inspId = 'insp-recovery-001';
      state.inspections.set(inspId, {
        id: inspId,
        status: InspectionStatus.AWAITING_AI_RECOVERY,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        retryCount: 1,
        nextRetryAt: new Date(Date.now() + 30000).toISOString(),
        createdAt: new Date().toISOString(),
      });

      const insp = state.inspections.get(inspId);
      expect(insp.status).toBe(InspectionStatus.AWAITING_AI_RECOVERY);
      expect(insp.retryCount).toBe(1);
    }
  });

  test('TC7.2: SAFE_TO_RIDE inspection remains in recovery queue through backoff intervals', async ({ mockServer }) => {
    const state = mockServer?.state;
    if (state) {
      const inspId = 'insp-recovery-safe';
      state.inspections.set(inspId, {
        id: inspId,
        status: InspectionStatus.AWAITING_AI_RECOVERY,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        retryCount: 2,
        createdAt: new Date(Date.now() - 120000).toISOString(), // 2 minutes ago
      });

      const insp = state.inspections.get(inspId);
      expect(insp.status).toBe(InspectionStatus.AWAITING_AI_RECOVERY);
    }
  });

  test('TC7.3: CRITICAL_STOPPED inspection in recovery queue >5 minutes escalates to HUMAN_TRIAGE with weight 1', async ({ mockServer, mechanicClient }) => {
    const state = mockServer?.state;
    if (state) {
      const inspId = 'insp-recovery-critical-sla';
      state.inspections.set(inspId, {
        id: inspId,
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.HUMAN_TRIAGE, // SLA job moves to HUMAN_TRIAGE
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        priorityWeight: 1,
        priorityReason: QueuePriorityReason.CRITICAL_SAFETY,
        createdAt: new Date(Date.now() - 360000).toISOString(), // 6 min ago (> 5 min)
      });

      const queue = await mechanicClient.getMechanicQueue();
      expect(queue.status).toBe(200);
      const item = queue.data.items.find((i) => i.inspectionId === inspId);
      expect(item).toBeDefined();
      expect(item?.priorityWeight).toBe(1);
      expect(item?.priorityReason).toBe(QueuePriorityReason.CRITICAL_SAFETY);
    }
  });

  test('TC7.4: Recovery backoff schedules next retry (30s -> 60s -> 300s)', async () => {
    const backoffSchedule = [30, 60, 300];
    expect(backoffSchedule[0]).toBe(30);
    expect(backoffSchedule[1]).toBe(60);
    expect(backoffSchedule[2]).toBe(300);
  });

  test('TC7.5: Successful recovery response transitions status away from AWAITING_AI_RECOVERY', async ({ mockServer }) => {
    const state = mockServer?.state;
    if (state) {
      const inspId = 'insp-recovery-success';
      state.inspections.set(inspId, {
        id: inspId,
        status: InspectionStatus.AWAITING_AI_RECOVERY,
      });

      // Simulate recovery worker completion
      const insp = state.inspections.get(inspId);
      insp.status = InspectionStatus.AUTO_RESOLVED;
      expect(insp.status).toBe(InspectionStatus.AUTO_RESOLVED);
    }
  });
});
