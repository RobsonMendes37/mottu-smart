import { test, expect } from '../../src/fixtures/test-context.js';
import { ReportedOriginContext, SafetyRiskLevel, InspectionStatus } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 2: Boundary 08 - Contestation Length & Status Boundaries', () => {
  test('TB8.1: EXACT text length boundary: reason with exactly 10 characters is ACCEPTED', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-exact10.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const exactly10 = '1234567890'; // length 10
    const res = await riderClient.contestInspection(created.data.inspectionId, { reason: exactly10 });
    expect(res.status).toBe(200);
    expect(res.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
  });

  test('TB8.2: EXACT text length boundary: reason with exactly 9 characters is REJECTED with 400 Bad Request', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-exact9.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const exactly9 = '123456789'; // length 9
    const res = await riderClient.contestInspection(created.data.inspectionId, { reason: exactly9 });
    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
  });

  test('TB8.3: Contestation reason with whitespace padding (10 spaces) is trimmed and rejected with 400', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-spaces.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const tenSpaces = '          ';
    const res = await riderClient.contestInspection(created.data.inspectionId, { reason: tenSpaces });
    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
  });

  test('TB8.4: Contestation reason with exactly 500 characters (max limit) is accepted', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-500chars.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const maxReason = 'A'.repeat(500);
    const res = await riderClient.contestInspection(created.data.inspectionId, { reason: maxReason });
    expect(res.status).toBe(200);
  });

  test('TB8.5: Attempting contestation on inspection not in AUTO_RESOLVED status returns 422', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    // Inspection created with fraud flag goes straight to HUMAN_TRIAGE
    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-cannot-contest.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const res = await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'Tentando contestar chamado que já está em triagem humana.',
    });
    expect(res.status).toBe(422);
    ContractValidator.assertProblemDetail(res.data, 422);
  });
});
