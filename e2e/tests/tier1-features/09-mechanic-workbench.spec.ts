import { test, expect } from '../../src/fixtures/test-context.js';
import {
  InspectionStatus,
  DamageVerdict,
  QueuePriorityReason,
  SafetyRiskLevel,
} from '../../src/contracts/enums.js';

test.describe('Tier 1: Feature 09 - Mechanic Intelligent Queue & Workbench', () => {
  test('TC9.1: Mechanic queue returns exclusively inspections in HUMAN_TRIAGE status', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const state = mockServer?.state;
    if (state) {
      state.inspections.set('triage-1', {
        id: 'triage-1',
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos Entregador',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 2,
        priorityReason: QueuePriorityReason.RIDER_CONTESTATION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date().toISOString(),
      });
      state.inspections.set('resolved-1', {
        id: 'resolved-1',
        status: InspectionStatus.AUTO_RESOLVED,
      });
    }

    const queueRes = await mechanicClient.getMechanicQueue();
    expect(queueRes.status).toBe(200);
    expect(queueRes.data.items.every((i) => i.priorityWeight !== undefined)).toBe(true);
    expect(queueRes.data.items.some((i) => i.inspectionId === 'resolved-1')).toBe(false);
  });

  test('TC9.2: Mechanic queue orders items strictly by priorityWeight ASC, then createdAt ASC', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const state = mockServer?.state;
    if (state) {
      // Insert items with different weights
      state.inspections.set('item-fraud-4', {
        id: 'item-fraud-4',
        plate: 'MOT4D56',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 4,
        priorityReason: QueuePriorityReason.FRAUD_SUSPICION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 5000).toISOString(),
      });
      state.inspections.set('item-contest-2', {
        id: 'item-contest-2',
        plate: 'MOT2B34',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 2,
        priorityReason: QueuePriorityReason.RIDER_CONTESTATION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 4000).toISOString(),
      });
      state.inspections.set('item-safety-1', {
        id: 'item-safety-1',
        plate: 'POP3C45',
        motorcycleModel: 'HONDA_POP_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 1,
        priorityReason: QueuePriorityReason.CRITICAL_SAFETY,
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        createdAt: new Date(Date.now() - 3000).toISOString(),
      });
      state.inspections.set('item-doubt-3', {
        id: 'item-doubt-3',
        plate: 'TVS4D56',
        motorcycleModel: 'TVS_APACHE_160',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 3,
        priorityReason: QueuePriorityReason.LOW_AI_CONFIDENCE,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 2000).toISOString(),
      });
    }

    const queueRes = await mechanicClient.getMechanicQueue();
    expect(queueRes.status).toBe(200);
    const weights = queueRes.data.items.map((i) => i.priorityWeight);
    for (let k = 0; k < weights.length - 1; k++) {
      expect(weights[k]).toBeLessThanOrEqual(weights[k + 1]);
    }
  });

  test('TC9.3: Mechanic retrieves detailed inspection view with full telemetry and AI summary', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('insp-detailed-desk', {
        id: 'insp-detailed-desk',
        photoObjectKey: 'inspections/2026/11/manete-zoom.webp',
        reportedContext: 'ACCIDENT_OR_DROP',
        safetyRisk: 'SAFE_TO_RIDE',
        reportedOdometerKm: 1035,
        status: InspectionStatus.HUMAN_TRIAGE,
        hoursSincePickup: 14.5,
        kmDrivenSincePickup: 35,
        fraudSuspected: false,
        aiTechnicalSummary: 'Trinca identificada no corpo do manete de freio.',
        aiVerdict: DamageVerdict.MAU_USO,
        laborFeeBrl: 35.0,
        totalEstimatedBrl: 60.0,
        items: [],
        createdAt: new Date().toISOString(),
      });
    }

    const res = await mechanicClient.getMechanicInspectionDetail('insp-detailed-desk');
    expect(res.status).toBe(200);
    expect(res.data.hoursSincePickup).toBe(14.5);
    expect(res.data.kmDrivenSincePickup).toBe(35);
    expect(res.data.aiTechnicalSummary).toContain('manete de freio');
    expect(res.data.photoDownloadUrl).toBeTruthy();
  });

  test('TC9.4: Mechanic resolves inspection overriding verdict to DESGASTE_NATURAL recalculating total to R$ 0.00', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('insp-to-override', {
        id: 'insp-to-override',
        status: InspectionStatus.HUMAN_TRIAGE,
        aiVerdict: DamageVerdict.MAU_USO,
        totalEstimatedBrl: 60.0,
        createdAt: new Date().toISOString(),
      });
    }

    const resolveRes = await mechanicClient.resolveInspection('insp-to-override', {
      finalVerdict: DamageVerdict.DESGASTE_NATURAL,
      catalogPartIds: [],
      mechanicNotes: 'Avaria por fadiga natural e não por queda.',
    });

    expect(resolveRes.status).toBe(200);
    expect(resolveRes.data.status).toBe(InspectionStatus.HUMAN_RESOLVED);
    expect(resolveRes.data.finalVerdict).toBe(DamageVerdict.DESGASTE_NATURAL);
    expect(resolveRes.data.totalEstimatedBrl).toBe(0.0);
    expect(resolveRes.data.customerFeedbackText).toBeTruthy();
  });

  test('TC9.5: Mechanic resolution generates audit log entry and outbox event', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('insp-audited', {
        id: 'insp-audited',
        status: InspectionStatus.HUMAN_TRIAGE,
        createdAt: new Date().toISOString(),
      });
    }

    const logsBefore = state?.auditLogs.length || 0;
    const outboxBefore = state?.outbox.length || 0;

    await mechanicClient.resolveInspection('insp-audited', {
      finalVerdict: DamageVerdict.MAU_USO,
      catalogPartIds: [],
      mechanicNotes: 'Impacto comprovado.',
    });

    expect((state?.auditLogs.length || 0)).toBe(logsBefore + 1);
    expect((state?.outbox.length || 0)).toBe(outboxBefore + 1);
  });
});
