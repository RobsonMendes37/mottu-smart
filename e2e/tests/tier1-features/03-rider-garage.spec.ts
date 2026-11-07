import { test, expect } from '../../src/fixtures/test-context.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 1: Feature 03 - Rider Garage & Active Contract', () => {
  test('TC3.1: Rider with active rental contract sees motorcycle telemetry in garage', async ({ riderClient }) => {
    const res = await riderClient.getGarage();
    expect(res.status).toBe(200);
    expect(res.data.activeContract).not.toBeNull();
    const contract = res.data.activeContract!;
    expect(contract.motorcycle.plate).toBe('MOT1A23');
    expect(contract.initialOdometerKm).toBe(1000);
    expect(contract.motorcycle.currentOdometerKm).toBeGreaterThanOrEqual(1000);
    expect(contract.motorcycle.yardName).toBeTruthy();
  });

  test('TC3.2: Garage displays open inspections count as a non-negative integer', async ({ riderClient }) => {
    const res = await riderClient.getGarage();
    expect(res.status).toBe(200);
    expect(typeof res.data.openInspectionsCount).toBe('number');
    expect(res.data.openInspectionsCount).toBeGreaterThanOrEqual(0);
    expect(res.data.openInspectionsCount).toBeLessThanOrEqual(3);
  });

  test('TC3.3: Garage displays recent inspections array', async ({ riderClient }) => {
    const res = await riderClient.getGarage();
    expect(res.status).toBe(200);
    expect(Array.isArray(res.data.recentInspections)).toBe(true);
  });

  test('TC3.4: Mechanic accessing rider garage receives 403 Forbidden', async ({ mechanicClient }) => {
    const res = await mechanicClient.getGarage();
    expect(res.status).toBe(403);
    ContractValidator.assertProblemDetail(res.data, 403);
  });

  test('TC3.5: Manager accessing rider garage receives 403 Forbidden', async ({ managerClient }) => {
    const res = await managerClient.getGarage();
    expect(res.status).toBe(403);
    ContractValidator.assertProblemDetail(res.data, 403);
  });
});
