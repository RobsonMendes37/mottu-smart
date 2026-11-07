import { test, expect } from '../../src/fixtures/test-context.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 1: Feature 04 - Photo S3 Upload & Pre-Validation', () => {
  test('TC4.1: Request presigned upload URL for valid image/webp returns 200 OK with photoObjectKey', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/webp',
      fileSizeBytes: 385000,
    });

    expect(res.status).toBe(200);
    expect(res.data.uploadUrl).toBeTruthy();
    expect(res.data.uploadUrl).toContain('mottu-inspections');
    expect(res.data.photoObjectKey).toBeTruthy();
    expect(res.data.photoObjectKey).toMatch(/^inspections\/\d{4}\/\d{2}\/.*\.webp$/);
    expect(res.data.expiresInSeconds).toBeGreaterThanOrEqual(60);
  });

  test('TC4.2: Request presigned upload URL for valid image/jpeg returns 200 OK', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/jpeg',
      fileSizeBytes: 1200000,
    });

    expect(res.status).toBe(200);
    expect(res.data.photoObjectKey).toContain('.jpeg');
  });

  test('TC4.3: Request presigned upload URL for valid image/png returns 200 OK', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/png',
      fileSizeBytes: 2048000,
    });

    expect(res.status).toBe(200);
    expect(res.data.photoObjectKey).toContain('.png');
  });

  test('TC4.4: Reject unsupported content type (application/pdf) with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'application/pdf',
      fileSizeBytes: 500000,
    });

    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
    expect(res.data.detail).toContain('Formato de imagem inválido');
  });

  test('TC4.5: Reject oversized file (> 10 MB = 10485760 bytes) with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.getUploadUrl({
      contentType: 'image/jpeg',
      fileSizeBytes: 10485761,
    });

    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
    expect(res.data.detail).toContain('excede 10 MB');
  });
});
