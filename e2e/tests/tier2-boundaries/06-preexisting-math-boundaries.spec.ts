import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  InspectionStatus,
  DamageVerdict,
  QueuePriorityReason,
} from '../../src/contracts/enums.js';

test.describe('Tier 2: Boundary 06 - Pre-Existing Math & AI Threshold Boundaries', () => {
  test('TB6.1: EXACT boundary: 48.0h and 150km (hours <= 48.0 is TRUE) -> AUTO_RESOLVED R$ 0.00', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/boundary-48-0h.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1150, // 150km (> 100km)
      simulatedHoursSincePickup: 48.0, // EXACT <= 48.0h limit
    } as any);

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.PRE_EXISTENTE);
    expect(detail.data.totalEstimatedBrl).toBe(0.0);
  });

  test('TB6.2: EXACT boundary: 72.0h and 100km (km <= 100 is TRUE) -> AUTO_RESOLVED R$ 0.00', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/boundary-100km.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1100, // EXACT 100km driven (<= 100km)
      simulatedHoursSincePickup: 72.0, // > 48.0h
    } as any);

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.finalVerdict).toBe(DamageVerdict.PRE_EXISTENTE);
    expect(detail.data.totalEstimatedBrl).toBe(0.0);
  });

  test('TB6.3: EXACT boundary: 48.1h AND 101km (BOTH exceeded) -> HUMAN_TRIAGE for mechanic review', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/boundary-exceeded.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1101, // 101km (> 100km)
      simulatedHoursSincePickup: 48.1, // 48.1h (> 48.0h)
    } as any);

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
  });

  test('TB6.4: EXACT AI confidence boundary: confidence = 0.80 (>= 0.80) -> AUTO_RESOLVED', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/boundary-conf-80.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 2000,
      simulatedAiConfidence: 0.80, // EXACT threshold
    } as any);

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.AUTO_RESOLVED);
    expect(detail.data.aiConfidence).toBe(0.80);
  });

  test('TB6.5: EXACT AI confidence boundary: confidence = 0.79 (< 0.80) -> HUMAN_TRIAGE (LOW_AI_CONFIDENCE)', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const res = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/boundary-conf-79.webp',
      reportedContext: ReportedOriginContext.NORMAL_USE_WEAR,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 2000,
      simulatedAiConfidence: 0.79, // JUST BELOW threshold
    } as any);

    expect(res.status).toBe(202);
    const detail = await riderClient.getInspection(res.data.inspectionId);
    expect(detail.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
    expect(detail.data.aiConfidence).toBe(0.79);
  });
});
