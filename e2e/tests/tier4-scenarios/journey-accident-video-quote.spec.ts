import { test, expect } from '../../src/fixtures/test-context.js';
import crypto from 'node:crypto';

test.describe('Tier 4: Persona Journey - Multimodal Accident Video Reporting, Towing & Workshop Confirmation', () => {
  test('Journey: Rider reports crash with video -> Gets immediate tiered quote -> Workshop mechanic confirms dossier', async ({
    riderClient,
    mechanicClient,
    mockServer,
  }) => {
    // Phase 1: Rider in the street requests video upload URL
    const videoUploadRes = await riderClient.getAccidentUploadVideoUrl({
      contentType: 'video/mp4',
      fileSize: 18 * 1024 * 1024, // 18MB <= 30MB
      durationSeconds: 22, // 22s <= 30s
    });
    expect(videoUploadRes.status).toBe(200);
    const videoObjectKey = videoUploadRes.data.videoObjectKey;

    // Phase 2: Rider requests optional close photo upload URL
    const photoUploadRes = await riderClient.getAccidentUploadPhotoUrl({
      contentType: 'image/webp',
      fileSize: 1.5 * 1024 * 1024,
    });
    expect(photoUploadRes.status).toBe(200);
    const photoObjectKey = photoUploadRes.data.photoObjectKey;

    // Phase 3: Rider submits complete accident report
    const idempotencyKey = crypto.randomUUID();
    const submitRes = await riderClient.createAccidentReport(
      {
        videoObjectKey,
        photoObjectKeys: [photoObjectKey],
        hasInjuries: false,
        involvesThirdParty: true,
        policeReportNumber: 'BO-2026-998877',
        drivability: 'NEEDS_TOWING',
        reportedOdometerKm: 1450,
        description: 'Colisão lateral leve com outro entregador, manete e guidão danificados.',
      },
      idempotencyKey
    );

    expect(submitRes.status).toBe(201);
    const accidentId = submitRes.data.accidentReportId;
    expect(accidentId).toBeTruthy();

    // Phase 4: Rider inspects immediate budget estimation (SSE / REST)
    const riderDetail = await riderClient.getRiderAccident(accidentId);
    expect(riderDetail.status).toBe(200);
    expect(riderDetail.data.status).toBe('ESTIMATED_PENDING_WORKSHOP');
    expect(riderDetail.data.drivability).toBe('NEEDS_TOWING');
    expect(riderDetail.data.towingFeeBrl).toBe(80.0);
    expect(riderDetail.data.partsSubtotalBrl).toBe(193.0);
    expect(riderDetail.data.laborFeeBrl).toBe(35.0); // 2 parts = R$ 35
    expect(riderDetail.data.totalCostBrl).toBe(308.0); // 193 + 35 + 80
    expect(riderDetail.data.items.length).toBe(2);

    // Motorcycle is locked to maintenance due to towing
    const garage = await riderClient.getGarage();
    expect(garage.data.activeContract).not.toBeNull();
    expect(garage.data.activeContract!.motorcycle.status).toBe('IN_MAINTENANCE');

    // Phase 5: Mechanic receives accident dossier in the workshop console
    const queueRes = await mechanicClient.getMechanicAccidentQueue();
    expect(queueRes.status).toBe(200);
    const queuedAccident = queueRes.data.items.find((item) => item.id === accidentId);
    expect(queuedAccident).toBeDefined();
    expect(queuedAccident?.totalCostBrl).toBe(308.0);

    const dossierRes = await mechanicClient.getMechanicAccidentDetail(accidentId);
    expect(dossierRes.status).toBe(200);
    expect(dossierRes.data.videoDownloadUrl).toBeTruthy();
    expect(dossierRes.data.riderName).toBeTruthy();
    expect(dossierRes.data.items[0].videoTimestampSeconds).toBe(4);
    expect(dossierRes.data.items[1].videoTimestampSeconds).toBe(8);

    // Phase 6: Mechanic physically inspects bike, checks video evidence and confirms both parts
    const allPartIds = dossierRes.data.items.map((i) => i.id);
    const confirmRes = await mechanicClient.confirmMechanicAccident(accidentId, {
      confirmedPartIds: allPartIds,
      mechanicNotes: 'Conferência física realizada no galpão. Ambos os componentes necessitam de troca imediata.',
    });

    expect(confirmRes.status).toBe(200);
    expect(confirmRes.data.status).toBe('WORKSHOP_CONFIRMED');
    expect(confirmRes.data.confirmedTotalCostBrl).toBe(308.0);

    // Phase 7: Rider sees status updated to WORKSHOP_CONFIRMED
    const finalRiderDetail = await riderClient.getRiderAccident(accidentId);
    expect(finalRiderDetail.status).toBe(200);
    expect(finalRiderDetail.data.status).toBe('WORKSHOP_CONFIRMED');
    expect(finalRiderDetail.data.totalCostBrl).toBe(308.0);

    // Phase 8: Outbox contains analytics event ready for BigQuery sync
    if (mockServer) {
      const outbox = (mockServer as any).state.outbox;
      const event = outbox.find((e: any) => e.accidentReportId === accidentId);
      expect(event).toBeDefined();
      expect(event.eventType).toBe('ACCIDENT_REPORT_CONFIRMED');
      expect(event.payload.totalCostBrl).toBe(308.0);
    }
  });
});
