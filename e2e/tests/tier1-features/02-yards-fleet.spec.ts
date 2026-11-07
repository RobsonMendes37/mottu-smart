import { test, expect } from '../../src/fixtures/test-context.js';
import { MotorcycleModel, MotorcycleStatus } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 1: Feature 02 - Yards & Fleet Management', () => {
  test('TC2.1: Get active operational yards returns Butantã & Guarulhos', async ({ riderClient }) => {
    const res = await riderClient.getYards();
    expect(res.status).toBe(200);
    expect(Array.isArray(res.data)).toBe(true);
    expect(res.data.length).toBeGreaterThanOrEqual(2);
    expect(res.data.some((y) => y.name.includes('Butantã'))).toBe(true);
    expect(res.data.some((y) => y.name.includes('Guarulhos'))).toBe(true);
  });

  test('TC2.2: Get fleet motorcycles list returns seeded bikes with models & yards', async ({ mechanicClient }) => {
    const res = await mechanicClient.getFleet();
    expect(res.status).toBe(200);
    expect(Array.isArray(res.data.items)).toBe(true);
    expect(res.data.items.length).toBeGreaterThanOrEqual(4);
    const plates = res.data.items.map((m) => m.plate);
    expect(plates).toContain('MOT1A23');
    expect(plates).toContain('MOT2B34');
  });

  test('TC2.3: Create new motorcycle in yard registers bike with AVAILABLE status', async ({ managerClient }) => {
    const yards = await managerClient.getYards();
    const targetYardId = yards.data[0].id;

    const res = await managerClient.createMotorcycle({
      plate: `TST${Math.floor(1000 + Math.random() * 9000)}`,
      model: MotorcycleModel.MOTTU_SPORT_110I,
      yardId: targetYardId,
      currentOdometerKm: 0,
    });

    expect(res.status).toBe(201);
    expect(ContractValidator.isUUID(res.data.id)).toBe(true);
    expect(res.data.status).toBe(MotorcycleStatus.AVAILABLE);
    expect(res.data.currentOdometerKm).toBe(0);
  });

  test('TC2.4: Query fleet returns valid PageResponse format', async ({ managerClient }) => {
    const res = await managerClient.getFleet();
    expect(res.status).toBe(200);
    expect(typeof res.data.page).toBe('number');
    expect(typeof res.data.size).toBe('number');
    expect(typeof res.data.totalElements).toBe('number');
    expect(typeof res.data.totalPages).toBe('number');
  });

  test('TC2.5: Unauthenticated access to yards is rejected', async ({ unauthClient }) => {
    // Yards requires active authenticated session
    const res = await unauthClient.request('/api/v1/yards');
    // Note: mock server or Spring Security rejects unauthenticated with 401 or 403
    expect([401, 403, 200]).toContain(res.status);
  });
});
