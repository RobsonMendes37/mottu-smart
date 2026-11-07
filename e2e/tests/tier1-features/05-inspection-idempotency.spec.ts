import { test, expect } from '../../src/fixtures/test-context.js';
import { ReportedOriginContext, SafetyRiskLevel } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';
import crypto from 'node:crypto';

test.describe('Tier 1: Feature 05 - Inspection Ingestion & Idempotency', () => {
  test('TC5.1: Create inspection with unique Idempotency-Key returns 202 Accepted and inspectionId', async ({ riderClient }) => {
    const key = crypto.randomUUID();
    const res = await riderClient.createInspection({
      photoObjectKey: `inspections/2026/11/${crypto.randomUUID()}.webp`,
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1020,
      riderNotes: 'Teste de abertura única',
    }, key);

    expect(res.status).toBe(202);
    expect(ContractValidator.isUUID(res.data.inspectionId)).toBe(true);
    expect(res.data.status).toBeTruthy();
  });

  test('TC5.2: Replaying identical request with same Idempotency-Key returns existing inspectionId without duplication', async ({ riderClient }) => {
    const key = `idempotent-replay-${Date.now()}`;
    const payload = {
      photoObjectKey: `inspections/2026/11/${crypto.randomUUID()}.webp`,
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1020,
      riderNotes: 'Clique repetido de 3G',
    };

    const first = await riderClient.createInspection(payload, key);
    expect(first.status).toBe(202);

    const second = await riderClient.createInspection(payload, key);
    expect(second.status).toBe(202);
    expect(second.data.inspectionId).toBe(first.data.inspectionId);

    const third = await riderClient.createInspection(payload, key);
    expect(third.status).toBe(202);
    expect(third.data.inspectionId).toBe(first.data.inspectionId);
  });

  test('TC5.3: Request missing Idempotency-Key header is rejected with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.request('/api/v1/rider/inspections', {
      method: 'POST',
      body: {
        photoObjectKey: 'inspections/2026/11/no-key.webp',
        reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        reportedOdometerKm: 1030,
      },
      // Intentionally omitting idempotencyKey
    });

    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
  });

  test('TC5.4: Enforce maximum of 3 open inspections per contract', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    // Submit 3 separate open inspections (fraud flag places them in HUMAN_TRIAGE which stays open)
    for (let i = 1; i <= 3; i++) {
      const res = await riderClient.createInspection({
        photoObjectKey: `inspections/2026/11/fraud-batch-${i}.webp`,
        reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
        safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
        reportedOdometerKm: 1050 + i,
      }, `key-batch-${Date.now()}-${i}`);
      expect(res.status).toBe(202);
    }

    // 4th open inspection must be rejected with 422
    const fourth = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/insp-4.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1060,
    }, `key-fourth-${Date.now()}`);

    expect(fourth.status).toBe(422);
    ContractValidator.assertProblemDetail(fourth.data, 422);
    expect(fourth.data.detail).toContain('Máximo de 3 chamados em andamento');
  });

  test('TC5.5: Resolving an inspection decrements open inspections count', async ({ riderClient, mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-to-resolve.webp', // enters HUMAN_TRIAGE
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1010,
    });
    expect(created.status).toBe(202);

    const garageBefore = await riderClient.getGarage();
    expect(garageBefore.data.openInspectionsCount).toBeGreaterThanOrEqual(1);

    // Mechanic resolves it
    const resolved = await mechanicClient.resolveInspection(created.data.inspectionId, {
      finalVerdict: ReportedOriginContext.RECEIVED_LIKE_THIS as any,
      catalogPartIds: [],
      mechanicNotes: 'Aprovado pelo mecânico',
    });
    expect(resolved.status).toBe(200);

    const garageAfter = await riderClient.getGarage();
    expect(garageAfter.data.openInspectionsCount).toBe(garageBefore.data.openInspectionsCount - 1);
  });
});
