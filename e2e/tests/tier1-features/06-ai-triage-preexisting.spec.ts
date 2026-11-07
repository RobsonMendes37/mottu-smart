import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  InspectionStatus,
  DamageVerdict,
} from '../../src/contracts/enums.js';

test.describe('Tier 1: Feature 06 - AI Triage & Pre-Existing Rule Engine', () => {
  test('TC6.1: Pre-existing damage claim under 48h and 100km is AUTO_RESOLVED with R$ 0.00 total', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-riscado.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1035, // 35 km since pickup (<= 100km)
      riderNotes: 'Manete já estava ralado quando retirei hoje',
    });

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.status).toBe(200);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.PRE_EXISTENTE);
    expect(detail.data.laborFeeBrl).toBe(0.0);
    expect(detail.data.totalEstimatedBrl).toBe(0.0);
  });

  test('TC6.2: Natural wear and tear claim is AUTO_RESOLVED with R$ 0.00 total and preventive replacement', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/lona-freio.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 6000,
      riderNotes: 'Freio chiando após 5000 km rodados',
    });

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.DESGASTE_NATURAL);
    expect(detail.data.totalEstimatedBrl).toBe(0.0);
  });

  test('TC6.3: Accident or drop claim calculates item price + standard labor fee (R$ 35.00)', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/queda-lateral.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1200,
      riderNotes: 'Moto tombou ao estacionar',
    });

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.MAU_USO);
    expect(detail.data.laborFeeBrl).toBe(35.0);
    expect(detail.data.totalEstimatedBrl).toBe(60.0); // 25 (part) + 35 (labor)
  });

  test('TC6.4: High confidence AI result (>= 0.80) completes automatically without human queue', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/foto-clara.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1500,
    });

    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.aiConfidence).toBeGreaterThanOrEqual(0.8);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
  });

  test('TC6.5: Pre-existing claim exceeding 48h AND 100km diverts to HUMAN_TRIAGE for mechanic evaluation', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/farol-trincado.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1250, // 250 km since initial (> 100km)
      riderNotes: 'Notei isso após 3 semanas',
      simulatedHoursSincePickup: 48.1, // > 48.0h
    } as any);

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
  });
});
