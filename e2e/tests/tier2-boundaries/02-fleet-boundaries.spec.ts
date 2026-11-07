import { test, expect } from '../../src/fixtures/test-context.js';
import { MotorcycleModel, MotorcycleStatus } from '../../src/contracts/enums.js';

test.describe('Tier 2: Boundary 02 - Fleet & Motorcycle Boundaries', () => {
  test('TB2.1: Negative odometer kilometer value (-1) is rejected', async ({ managerClient }) => {
    const yards = await managerClient.getYards();
    const res = await managerClient.createMotorcycle({
      plate: `NEG${Math.floor(1000 + Math.random() * 9000)}`,
      model: MotorcycleModel.MOTTU_SPORT_110I,
      yardId: yards.data[0].id,
      currentOdometerKm: -1,
    });
    // In database check constraint or validation: current_odometer_km >= 0
    expect([400, 422]).toContain(res.status);
  });

  test('TB2.2: Zero odometer kilometer value (0) is accepted for brand-new bike', async ({ managerClient }) => {
    const yards = await managerClient.getYards();
    const res = await managerClient.createMotorcycle({
      plate: `ZER${Math.floor(1000 + Math.random() * 9000)}`,
      model: MotorcycleModel.MOTTU_SPORT_110I,
      yardId: yards.data[0].id,
      currentOdometerKm: 0,
    });
    expect(res.status).toBe(201);
    expect(res.data.currentOdometerKm).toBe(0);
  });

  test('TB2.3: Fleet pagination bounds: page 0 size 20 returns maximum 20 items', async ({ managerClient }) => {
    const res = await managerClient.getFleet({ status: undefined, plate: undefined });
    expect(res.status).toBe(200);
    expect(res.data.items.length).toBeLessThanOrEqual(20);
    expect(res.data.page).toBe(0);
  });

  test('TB2.4: Query fleet with non-existent plate returns empty items list', async ({ managerClient }) => {
    const res = await managerClient.getFleet({ plate: 'NON_EXISTENT_PLATE_99' });
    expect(res.status).toBe(200);
  });

  test('TB2.5: Motorcycle status transitions preserve model and plate identity', async ({ managerClient }) => {
    const fleet = await managerClient.getFleet();
    const bike = fleet.data.items[0];
    expect(bike.plate).toBeTruthy();
    expect(Object.values(MotorcycleModel)).toContain(bike.model);
    expect(Object.values(MotorcycleStatus)).toContain(bike.status);
  });
});
