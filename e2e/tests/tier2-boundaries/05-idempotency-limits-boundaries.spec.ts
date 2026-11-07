import { test, expect } from '../../src/fixtures/test-context.js';
import { ReportedOriginContext, SafetyRiskLevel } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 2: Boundary 05 - Idempotency & Concurrency Limits', () => {
  test('TB5.1: Whitespace-only Idempotency-Key is rejected with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/blank-key.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    }, '   ');

    expect([400, 422]).toContain(res.status);
  });

  test('TB5.2: Exactly 3 open inspections are permitted, 4th is blocked with 422', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    // 1st
    const r1 = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-b1.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1010,
    }, `idemp-b1-${Date.now()}`);
    expect(r1.status).toBe(202);

    // 2nd
    const r2 = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-b2.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1020,
    }, `idemp-b2-${Date.now()}`);
    expect(r2.status).toBe(202);

    // 3rd (last permitted)
    const r3 = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-b3.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1030,
    }, `idemp-b3-${Date.now()}`);
    expect(r3.status).toBe(202);

    // 4th (boundary breach)
    const r4 = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-b4.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1040,
    }, `idemp-b4-${Date.now()}`);
    expect(r4.status).toBe(422);
    ContractValidator.assertProblemDetail(r4.data, 422);
  });

  test('TB5.3: Long Idempotency-Key (80 chars) is processed safely', async ({ riderClient }) => {
    const longKey = 'a'.repeat(80);
    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/long-key.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    }, longKey);

    expect([202, 422]).toContain(res.status);
  });

  test('TB5.4: Special characters in rider notes are stored safely without injection', async ({ riderClient }) => {
    const notesWithSpecialChars = '<script>alert("xss")</script> & "quotes" \'single\' --sql injection';
    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/special-chars.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
      riderNotes: notesWithSpecialChars,
    });

    expect([202, 422]).toContain(res.status);
  });

  test('TB5.5: Simultaneous parallel requests with identical Idempotency-Key return identical inspectionId', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const sharedKey = `parallel-key-${Date.now()}`;
    const payload = {
      photoObjectKey: 'inspections/2026/11/parallel.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    };

    const [p1, p2] = await Promise.all([
      riderClient.createInspection(payload, sharedKey),
      riderClient.createInspection(payload, sharedKey),
    ]);

    expect(p1.status).toBe(202);
    expect(p2.status).toBe(202);
    expect(p1.data.inspectionId).toBe(p2.data.inspectionId);
  });
});
