import { test, expect } from '../../src/fixtures/test-context.js';

test.describe('Tier 1: Feature 12 - Executive Analytics & BigQuery Outbox Batch', () => {
  test('TC12.1: Manager queries analytics summary returning key operational KPIs', async ({ managerClient }) => {
    const res = await managerClient.getAnalyticsSummary();
    expect(res.status).toBe(200);
    expect(typeof res.data.activeMotorcyclesCount).toBe('number');
    expect(typeof res.data.autoResolvedPercentage).toBe('number');
    expect(typeof res.data.currentHumanQueueSize).toBe('number');
    expect(typeof res.data.estimatedMechanicHoursSaved).toBe('number');
  });

  test('TC12.2: Analytics summary includes valid Looker Studio embed URL', async ({ managerClient }) => {
    const res = await managerClient.getAnalyticsSummary();
    expect(res.status).toBe(200);
    expect(res.data.lookerStudioEmbedUrl).toBeTruthy();
    expect(res.data.lookerStudioEmbedUrl).toContain('lookerstudio.google.com');
  });

  test('TC12.3: Manager queries system settings returning standard labor fee', async ({ managerClient }) => {
    const res = await managerClient.getSettings();
    expect(res.status).toBe(200);
    expect(res.data.standardLaborFeeBrl).toBe(35.0);
  });

  test('TC12.4: Manager updates standard labor fee successfully', async ({ managerClient }) => {
    const updateRes = await managerClient.updateSettings({ standardLaborFeeBrl: 40.0 });
    expect(updateRes.status).toBe(200);
    expect(updateRes.data.standardLaborFeeBrl).toBe(40.0);

    // Revert back
    await managerClient.updateSettings({ standardLaborFeeBrl: 35.0 });
  });

  test('TC12.5: Rider or Mechanic attempting to read executive analytics receives 403 Forbidden', async ({ riderClient, mechanicClient }) => {
    const riderRes = await riderClient.getAnalyticsSummary();
    expect(riderRes.status).toBe(403);

    const mechanicRes = await mechanicClient.getAnalyticsSummary();
    expect(mechanicRes.status).toBe(403);
  });
});
