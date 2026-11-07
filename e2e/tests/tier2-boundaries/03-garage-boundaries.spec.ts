import { test, expect } from '../../src/fixtures/test-context.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 2: Boundary 03 - Rider Garage & Isolation Boundaries', () => {
  test('TB3.1: Rider cannot access other rider inspection detail by ID (IDOR boundary)', async ({ riderClient, mockServer }) => {
    // Other rider's inspection
    const otherId = 'other-rider-inspection-uuid';
    if (mockServer) {
      mockServer.state.inspections.set(otherId, {
        id: otherId,
        riderId: 'different-rider-id-999',
        status: 'AUTO_RESOLVED',
        photoObjectKey: 'inspections/2026/11/secret.webp',
      });
    }

    const res = await riderClient.getInspection(otherId);
    // Anti-IDOR: Must return 404 Not Found (or 403) to prevent leaking existence
    expect([404, 403]).toContain(res.status);
    ContractValidator.assertProblemDetail(res.data);
  });

  test('TB3.2: Garage with zero recent inspections returns empty array without throwing error', async ({ riderClient, mockServer }) => {
    if (mockServer) mockServer.resetState();
    const res = await riderClient.getGarage();
    expect(res.status).toBe(200);
    expect(Array.isArray(res.data.recentInspections)).toBe(true);
  });

  test('TB3.3: Initial odometer matches current odometer when 0 km driven', async ({ riderClient, mockServer }) => {
    if (mockServer) {
      const contract = mockServer.state.contracts[0];
      if (contract) {
        contract.initialOdometerKm = 1000;
        const moto = mockServer.state.motorcycles.find((m) => m.id === contract.motorcycleId);
        if (moto) moto.currentOdometerKm = 1000;
      }
    }

    const res = await riderClient.getGarage();
    expect(res.status).toBe(200);
    if (res.data.activeContract) {
      expect(res.data.activeContract.initialOdometerKm).toBe(res.data.activeContract.motorcycle.currentOdometerKm);
    }
  });

  test('TB3.4: Query non-existent inspection returns 404 Problem Details', async ({ riderClient }) => {
    const res = await riderClient.getInspection('00000000-0000-0000-0000-000000000000');
    expect(res.status).toBe(404);
    ContractValidator.assertProblemDetail(res.data, 404);
  });

  test('TB3.5: Garage contract contains valid ISO date timestamp for pickedUpAt', async ({ riderClient }) => {
    const res = await riderClient.getGarage();
    expect(res.status).toBe(200);
    if (res.data.activeContract) {
      expect(ContractValidator.isISODate(res.data.activeContract.pickedUpAt)).toBe(true);
    }
  });
});
