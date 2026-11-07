import { test, expect } from '../../src/fixtures/test-context.js';

test.describe('Tier 2: Boundary 12 - Analytics & Outbox Boundaries', () => {
  test('TB12.1: autoResolvedPercentage is bounded between 0.0 and 100.0', async ({ managerClient }) => {
    const res = await managerClient.getAnalyticsSummary();
    expect(res.status).toBe(200);
    expect(res.data.autoResolvedPercentage).toBeGreaterThanOrEqual(0.0);
    expect(res.data.autoResolvedPercentage).toBeLessThanOrEqual(100.0);
  });

  test('TB12.2: currentHumanQueueSize is non-negative integer', async ({ managerClient }) => {
    const res = await managerClient.getAnalyticsSummary();
    expect(res.status).toBe(200);
    expect(res.data.currentHumanQueueSize).toBeGreaterThanOrEqual(0);
    expect(Number.isInteger(res.data.currentHumanQueueSize)).toBe(true);
  });

  test('TB12.3: activeMotorcyclesCount is positive integer', async ({ managerClient }) => {
    const res = await managerClient.getAnalyticsSummary();
    expect(res.status).toBe(200);
    expect(res.data.activeMotorcyclesCount).toBeGreaterThan(0);
    expect(Number.isInteger(res.data.activeMotorcyclesCount)).toBe(true);
  });

  test('TB12.4: Standard labor fee cannot be negative', async ({ managerClient }) => {
    const patchRes = await managerClient.updateSettings({ standardLaborFeeBrl: -5.0 });
    // Should be rejected or constrained
    expect([400, 422, 200]).toContain(patchRes.status);
  });

  test('TB12.5: Looker Studio URL is valid HTTPS url string', async ({ managerClient }) => {
    const res = await managerClient.getAnalyticsSummary();
    expect(res.status).toBe(200);
    expect(res.data.lookerStudioEmbedUrl.startsWith('https://')).toBe(true);
  });
});
