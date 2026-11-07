import { test, expect } from '../../src/fixtures/test-context.js';
import crypto from 'node:crypto';

test.describe('Tier 1: Feature 13 - Accident Video Reporting & Automatic Budget Quotation', () => {
  test('TC13.1: Rider requests presigned upload URL for valid video (video/mp4, <=30MB, <=30s)', async ({ riderClient }) => {
    const res = await riderClient.getAccidentUploadVideoUrl({
      contentType: 'video/mp4',
      fileSize: 15 * 1024 * 1024, // 15 MB
      durationSeconds: 20,
    });

    expect(res.status).toBe(200);
    expect(res.data.uploadUrl).toBeTruthy();
    expect(res.data.videoObjectKey).toContain('accidents/videos/');
    expect(res.data.expiresAt).toBeTruthy();
  });

  test('TC13.2: Rejects invalid video content type (application/pdf) with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.getAccidentUploadVideoUrl({
      contentType: 'application/pdf',
      fileSize: 5 * 1024 * 1024,
      durationSeconds: 15,
    });

    expect(res.status).toBe(400);
    expect(res.data.detail).toContain('Tipo de arquivo não suportado');
  });

  test('TC13.3: Rejects oversized video (> 30 MB) with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.getAccidentUploadVideoUrl({
      contentType: 'video/mp4',
      fileSize: 35 * 1024 * 1024, // 35 MB
      durationSeconds: 25,
    });

    expect(res.status).toBe(400);
    expect(res.data.detail).toContain('30MB');
  });

  test('TC13.4: Rejects video with duration exceeding 30 seconds with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.getAccidentUploadVideoUrl({
      contentType: 'video/webm',
      fileSize: 10 * 1024 * 1024,
      durationSeconds: 45, // > 30s
    });

    expect(res.status).toBe(400);
    expect(res.data.detail).toContain('30 segundos');
  });

  test('TC13.5: Rider requests presigned upload URL for close-up photo (image/webp, <=10MB)', async ({ riderClient }) => {
    const res = await riderClient.getAccidentUploadPhotoUrl({
      contentType: 'image/webp',
      fileSize: 2 * 1024 * 1024,
    });

    expect(res.status).toBe(200);
    expect(res.data.uploadUrl).toBeTruthy();
    expect(res.data.photoObjectKey).toContain('accidents/photos/');
  });

  test('TC13.6: Rider submits accident report with DRIVABLE -> R$ 0.00 towing fee and tiered labor fee', async ({ riderClient }) => {
    const idempotencyKey = crypto.randomUUID();
    const createRes = await riderClient.createAccidentReport(
      {
        videoObjectKey: 'accidents/videos/sample-accident.mp4',
        photoObjectKeys: ['accidents/photos/close-handlebar.webp'],
        hasInjuries: false,
        involvesThirdParty: false,
        drivability: 'DRIVABLE',
        reportedOdometerKm: 1250,
        description: 'Queda leve em curva molhada no asfalto',
      },
      idempotencyKey
    );

    expect(createRes.status).toBe(201);
    expect(createRes.data.accidentReportId).toBeTruthy();
    expect(createRes.data.status).toBe('ESTIMATED_PENDING_WORKSHOP');

    // Retrieve details as Rider
    const detailRes = await riderClient.getRiderAccident(createRes.data.accidentReportId);
    expect(detailRes.status).toBe(200);
    expect(detailRes.data.drivability).toBe('DRIVABLE');
    expect(detailRes.data.towingFeeBrl).toBe(0.0);
    expect(detailRes.data.partsSubtotalBrl).toBe(193.0);
    expect(detailRes.data.laborFeeBrl).toBe(35.0); // 2 parts = R$ 35
    expect(detailRes.data.totalCostBrl).toBe(228.0); // 193 + 35 + 0
    expect(detailRes.data.items.length).toBe(2);
    expect(detailRes.data.items[0].videoTimestampSeconds).toBe(4);
  });

  test('TC13.7: Idempotency: repeating submission with same key returns existing quote', async ({ riderClient }) => {
    const idempotencyKey = crypto.randomUUID();
    const payload = {
      videoObjectKey: 'accidents/videos/sample-accident-2.mp4',
      hasInjuries: false,
      involvesThirdParty: false,
      drivability: 'DRIVABLE' as const,
      reportedOdometerKm: 1300,
    };

    const first = await riderClient.createAccidentReport(payload, idempotencyKey);
    expect(first.status).toBe(201);

    const second = await riderClient.createAccidentReport(payload, idempotencyKey);
    expect(second.status).toBe(200);
    expect(second.data.accidentReportId).toBe(first.data.accidentReportId);
    expect(second.data.message).toContain('Idempotência');
  });

  test('TC13.8: Rider submits accident with NEEDS_TOWING -> R$ 80.00 towing fee applied and bike IN_MAINTENANCE', async ({ riderClient }) => {
    const createRes = await riderClient.createAccidentReport({
      videoObjectKey: 'accidents/videos/sample-towing.mp4',
      hasInjuries: true,
      involvesThirdParty: true,
      policeReportNumber: 'BO-SP-2026-112233',
      drivability: 'NEEDS_TOWING',
      reportedOdometerKm: 1500,
    });

    expect(createRes.status).toBe(201);

    const detailRes = await riderClient.getRiderAccident(createRes.data.accidentReportId);
    expect(detailRes.status).toBe(200);
    expect(detailRes.data.drivability).toBe('NEEDS_TOWING');
    expect(detailRes.data.towingFeeBrl).toBe(80.0);
    expect(detailRes.data.totalCostBrl).toBe(308.0); // 193 + 35 + 80
    expect(detailRes.data.hasInjuries).toBe(true);
    expect(detailRes.data.policeReportNumber).toBe('BO-SP-2026-112233');

    // Telemetry reflects motorcycle status
    const garageRes = await riderClient.getGarage();
    expect(garageRes.status).toBe(200);
    expect(garageRes.data.activeContract).not.toBeNull();
    expect(garageRes.data.activeContract!.motorcycle.status).toBe('IN_MAINTENANCE');
  });

  test('TC13.9: Submitting accident without Idempotency-Key is rejected with 400 Bad Request', async ({ riderClient }) => {
    const res = await riderClient.request('/api/v1/rider/accidents', {
      method: 'POST',
      body: {
        videoObjectKey: 'accidents/videos/sample.mp4',
        drivability: 'DRIVABLE',
        reportedOdometerKm: 1000,
      },
    });

    expect(res.status).toBe(400);
    expect(res.data.detail).toContain('Idempotency-Key');
  });

  test('TC13.10: Mechanic accesses accident queue and inspects full video dossier', async ({ riderClient, mechanicClient }) => {
    const createRes = await riderClient.createAccidentReport({
      videoObjectKey: 'accidents/videos/dossier-test.mp4',
      hasInjuries: false,
      involvesThirdParty: false,
      drivability: 'DRIVABLE',
      reportedOdometerKm: 1200,
    });

    // Mechanic lists queue
    const queueRes = await mechanicClient.getMechanicAccidentQueue();
    expect(queueRes.status).toBe(200);
    expect(queueRes.data.items.length).toBeGreaterThan(0);

    const found = queueRes.data.items.find((item) => item.id === createRes.data.accidentReportId);
    expect(found).toBeDefined();

    // Mechanic views dossier
    const dossierRes = await mechanicClient.getMechanicAccidentDetail(createRes.data.accidentReportId);
    expect(dossierRes.status).toBe(200);
    expect(dossierRes.data.videoDownloadUrl).toBeTruthy();
    expect(dossierRes.data.riderName).toBeTruthy();
    expect(dossierRes.data.items.length).toBe(2);
  });

  test('TC13.11: Mechanic confirms physical inspection: updates quote and emits to analytics outbox', async ({ riderClient, mechanicClient, mockServer }) => {
    const createRes = await riderClient.createAccidentReport({
      videoObjectKey: 'accidents/videos/confirm-test.mp4',
      hasInjuries: false,
      involvesThirdParty: false,
      drivability: 'DRIVABLE',
      reportedOdometerKm: 1200,
    });

    const reportId = createRes.data.accidentReportId;
    const detail = await mechanicClient.getMechanicAccidentDetail(reportId);

    // Mechanic confirms only 1 part (e.g. handlebar)
    const confirmedPartId = detail.data.items[0].id;
    const confirmRes = await mechanicClient.confirmMechanicAccident(reportId, {
      confirmedPartIds: [confirmedPartId],
      mechanicNotes: 'Manete não foi danificado no galpão, apenas guidão desalinhado.',
    });

    expect(confirmRes.status).toBe(200);
    expect(confirmRes.data.status).toBe('WORKSHOP_CONFIRMED');
    // 1 part (145.0) + labor (35.0) + towing (0.0) = 180.0
    expect(confirmRes.data.confirmedTotalCostBrl).toBe(180.0);

    // Outbox event was published
    if (mockServer) {
      const outbox = (mockServer as any).state.outbox;
      const accidentEvent = outbox.find((o: any) => o.accidentReportId === reportId);
      expect(accidentEvent).toBeDefined();
      expect(accidentEvent.eventType).toBe('ACCIDENT_REPORT_CONFIRMED');
    }
  });

  test('TC13.12: RBAC isolation: Rider cannot access mechanic accident endpoints', async ({ riderClient }) => {
    const queueRes = await riderClient.getMechanicAccidentQueue();
    expect(queueRes.status).toBe(403);

    const confirmRes = await riderClient.confirmMechanicAccident('fake-id', {
      confirmedPartIds: [],
    });
    expect(confirmRes.status).toBe(403);
  });
});
