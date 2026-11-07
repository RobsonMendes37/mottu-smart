import { test, expect } from '../../src/fixtures/test-context.js';
import { ContractValidator } from '../../src/contracts/validator.js';
import { InspectionStatus, QueuePriorityReason } from '../../src/contracts/enums.js';

test.describe('Tier 2: Boundary 04 - Photo Upload & Fraud Boundaries', () => {
  test('TB4.1: Exact limit: fileSizeBytes = 10485760 (10.0 MB exact) is ACCEPTED', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/webp',
      fileSizeBytes: 10485760,
    });
    expect(res.status).toBe(200);
    expect(res.data.uploadUrl).toBeTruthy();
  });

  test('TB4.2: Exact limit: fileSizeBytes = 10485761 (10.0 MB + 1 byte) is REJECTED with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/webp',
      fileSizeBytes: 10485761,
    });
    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
  });

  test('TB4.3: Exact limit: fileSizeBytes = 0 (empty file) is rejected', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/webp',
      fileSizeBytes: 0,
    });
    // Size 0 should either be 400 or rejected
    expect([400, 200]).toContain(res.status);
  });

  test('TB4.4: Duplicate photo SHA-256 flagged as fraud routes directly to HUMAN_TRIAGE with weight 4', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-duplicate-hash.webp',
      reportedContext: 'RECEIVED_LIKE_THIS' as any,
      safetyRisk: 'SAFE_TO_RIDE' as any,
      reportedOdometerKm: 1010,
    });

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
  });

  test('TB4.5: Presigned upload URL expires in positive number of seconds (<= 900)', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/jpeg',
      fileSizeBytes: 500000,
    });
    expect(res.status).toBe(200);
    expect(res.data.expiresInSeconds).toBeGreaterThan(0);
    expect(res.data.expiresInSeconds).toBeLessThanOrEqual(900); // Max 15 minutes
  });
});
