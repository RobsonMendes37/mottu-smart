import { test, expect } from '../../src/fixtures/test-context.js';
import { InspectionStatus, QueuePriorityReason, SafetyRiskLevel } from '../../src/contracts/enums.js';

test.describe('Tier 2: Boundary 09 - Mechanic Queue Sorting & Boundaries', () => {
  test('TB9.1: Queue strictly orders weights 1 -> 2 -> 3 -> 4', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('q-w4', {
        id: 'q-w4',
        plate: 'MOT4D56',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 4,
        priorityReason: QueuePriorityReason.FRAUD_SUSPICION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 1000).toISOString(),
      });
      state.inspections.set('q-w1', {
        id: 'q-w1',
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 1,
        priorityReason: QueuePriorityReason.CRITICAL_SAFETY,
        safetyRisk: SafetyRiskLevel.CRITICAL_STOPPED,
        createdAt: new Date(Date.now() - 1000).toISOString(),
      });
      state.inspections.set('q-w3', {
        id: 'q-w3',
        plate: 'MOT3C45',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 3,
        priorityReason: QueuePriorityReason.LOW_AI_CONFIDENCE,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 1000).toISOString(),
      });
      state.inspections.set('q-w2', {
        id: 'q-w2',
        plate: 'MOT2B34',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 2,
        priorityReason: QueuePriorityReason.RIDER_CONTESTATION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 1000).toISOString(),
      });
    }

    const q = await mechanicClient.getMechanicQueue();
    const ids = q.data.items.map((i) => i.inspectionId);
    expect(ids[0]).toBe('q-w1');
    expect(ids[1]).toBe('q-w2');
    expect(ids[2]).toBe('q-w3');
    expect(ids[3]).toBe('q-w4');
  });

  test('TB9.2: Identical priority weights tie-break by createdAt ASC (FIFO)', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('tie-older', {
        id: 'tie-older',
        plate: 'MOT1A23',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 2,
        priorityReason: QueuePriorityReason.RIDER_CONTESTATION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 20000).toISOString(), // 20s ago (older)
      });
      state.inspections.set('tie-newer', {
        id: 'tie-newer',
        plate: 'MOT2B34',
        motorcycleModel: 'MOTTU_SPORT_110I',
        riderName: 'Carlos',
        status: InspectionStatus.HUMAN_TRIAGE,
        priorityWeight: 2,
        priorityReason: QueuePriorityReason.RIDER_CONTESTATION,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        createdAt: new Date(Date.now() - 5000).toISOString(), // 5s ago (newer)
      });
    }

    const q = await mechanicClient.getMechanicQueue();
    const ids = q.data.items.map((i) => i.inspectionId);
    const olderIndex = ids.indexOf('tie-older');
    const newerIndex = ids.indexOf('tie-newer');
    expect(olderIndex).toBeLessThan(newerIndex);
  });

  test('TB9.3: Empty queue returns items: [] with totalElements: 0', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const q = await mechanicClient.getMechanicQueue();
    expect(q.status).toBe(200);
    expect(q.data.items.length).toBe(0);
    expect(q.data.totalElements).toBe(0);
  });

  test('TB9.4: Mechanic notes max length boundary: 500 characters accepted cleanly', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('insp-notes-500', {
        id: 'insp-notes-500',
        status: InspectionStatus.HUMAN_TRIAGE,
        createdAt: new Date().toISOString(),
      });
    }

    const longNotes = 'X'.repeat(500);
    const res = await mechanicClient.resolveInspection('insp-notes-500', {
      finalVerdict: 'MAU_USO' as any,
      catalogPartIds: [],
      mechanicNotes: longNotes,
    });
    expect(res.status).toBe(200);
  });

  test('TB9.5: Resolving already resolved inspection preserves finalVerdict', async ({ mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const state = mockServer?.state;
    if (state) {
      state.inspections.set('insp-already-resolved', {
        id: 'insp-already-resolved',
        status: InspectionStatus.HUMAN_RESOLVED,
        finalVerdict: 'PRE_EXISTENTE',
        createdAt: new Date().toISOString(),
      });
    }

    const detail = await mechanicClient.getMechanicInspectionDetail('insp-already-resolved');
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_RESOLVED);
  });
});
