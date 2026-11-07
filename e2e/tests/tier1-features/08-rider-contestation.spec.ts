import { test, expect } from '../../src/fixtures/test-context.js';
import {
  ReportedOriginContext,
  SafetyRiskLevel,
  InspectionStatus,
  QueuePriorityReason,
} from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 1: Feature 08 - Rider Contestation ("Discordo")', () => {
  test('TC8.1: Contesting AUTO_RESOLVED inspection with valid reason transitions to HUMAN_TRIAGE with weight 2', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/queda-contestar.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
      riderNotes: 'Ralado leve',
    });
    expect(created.status).toBe(202);

    const contestRes = await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'O ralado já constava quando retirei a moto no galpão Butantã.',
    });

    expect(contestRes.status).toBe(200);
    expect(contestRes.data.status).toBe(InspectionStatus.HUMAN_TRIAGE);
    expect(contestRes.data.priorityReason).toBe(QueuePriorityReason.RIDER_CONTESTATION);
  });

  test('TC8.2: Contestation marks contestedOnce = true preventing repeat contestation', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/manete-dupla.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'Primeira contestação detalhada válida com mais de dez letras.',
    });

    const detail = await riderClient.getInspection(created.data.inspectionId);
    expect(detail.data.contestedOnce).toBe(true);
    expect(detail.data.canContest).toBe(false);
  });

  test('TC8.3: Attempting contestation with reason shorter than 10 characters returns 400 Bad Request', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/curto.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    const contestRes = await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'não fui', // only 7 characters
    });

    expect(contestRes.status).toBe(400);
    ContractValidator.assertProblemDetail(contestRes.data, 400);
    expect(contestRes.data.detail).toContain('mínimo 10 caracteres');
  });

  test('TC8.4: Attempting second contestation on already contested inspection returns 422 Unprocessable Entity', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/repetida.webp',
      reportedContext: ReportedOriginContext.ACCIDENT_OR_DROP,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1050,
    });

    // 1st contestation succeeds
    await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'Primeira justificativa perfeitamente válida.',
    });

    // 2nd contestation must fail with 422
    const secondRes = await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'Tentando contestar de novo pela segunda vez consecutiva.',
    });

    expect(secondRes.status).toBe(422);
    ContractValidator.assertProblemDetail(secondRes.data, 422);
  });

  test('TC8.5: Attempting contestation on already HUMAN_RESOLVED inspection returns 422', async ({ riderClient, mechanicClient, mockServer }) => {
    if (mockServer) mockServer.resetState();

    const created = await riderClient.createInspection({
      photoObjectKey: 'inspections/2026/11/fraud-to-mecanico.webp',
      reportedContext: ReportedOriginContext.RECEIVED_LIKE_THIS,
      safetyRisk: SafetyRiskLevel.SAFE_TO_RIDE,
      reportedOdometerKm: 1020,
    });

    // Mechanic resolves it
    await mechanicClient.resolveInspection(created.data.inspectionId, {
      finalVerdict: ReportedOriginContext.NORMAL_USE_WEAR as any,
      catalogPartIds: [],
      mechanicNotes: 'Laudo final do mecânico.',
    });

    // Rider attempts to contest resolved
    const contestRes = await riderClient.contestInspection(created.data.inspectionId, {
      reason: 'Quero contestar a decisão final tomada pelo mecânico.',
    });

    expect(contestRes.status).toBe(422);
  });
});
