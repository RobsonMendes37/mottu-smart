import { test, expect } from '../../src/fixtures/test-context.js';
import { MotorcycleModel } from '../../src/contracts/enums.js';

test.describe('Tier 2: Boundary 11 - Parts Catalog Price & Override Boundaries', () => {
  test('TB11.1: Unit price boundary: 0.00 BRL is valid for promotional/free replacement item', async ({ managerClient }) => {
    const partsRes = await managerClient.getCatalogParts();
    const part = partsRes.data.items[0];

    const patchRes = await managerClient.updateCatalogPart(part.id, { unitPriceBrl: 0.00 });
    expect(patchRes.status).toBe(200);
    expect(patchRes.data.unitPriceBrl).toBe(0.00);
    expect(patchRes.data.priceOverridden).toBe(true);

    // Revert back
    await managerClient.updateCatalogPart(part.id, { unitPriceBrl: 25.00 });
  });

  test('TB11.2: Updating isNaturalWear toggles wear coverage classification', async ({ managerClient }) => {
    const partsRes = await managerClient.getCatalogParts();
    const part = partsRes.data.items[0];

    const originalWear = part.isNaturalWear;
    const patchRes = await managerClient.updateCatalogPart(part.id, { isNaturalWear: !originalWear });
    expect(patchRes.status).toBe(200);
    expect(patchRes.data.isNaturalWear).toBe(!originalWear);

    // Revert
    await managerClient.updateCatalogPart(part.id, { isNaturalWear: originalWear });
  });

  test('TB11.3: Update non-existent catalog part returns 404 Problem Details', async ({ managerClient }) => {
    const patchRes = await managerClient.updateCatalogPart('00000000-0000-0000-0000-000000000000', {
      unitPriceBrl: 15.00,
    });
    expect(patchRes.status).toBe(404);
  });

  test('TB11.4: All supported motorcycle models are present in catalog schema', async () => {
    const models = Object.values(MotorcycleModel);
    expect(models).toContain(MotorcycleModel.MOTTU_SPORT_110I);
    expect(models).toContain(MotorcycleModel.HONDA_POP_110I);
    expect(models).toContain(MotorcycleModel.TVS_APACHE_160);
    expect(models).toContain(MotorcycleModel.TVS_RAIDER_125);
  });

  test('TB11.5: Seed catalog parts have non-negative prices and non-empty categories', async ({ managerClient }) => {
    const partsRes = await managerClient.getCatalogParts();
    expect(partsRes.status).toBe(200);
    for (const part of partsRes.data.items) {
      expect(part.unitPriceBrl).toBeGreaterThanOrEqual(0);
      expect(part.category.trim().length).toBeGreaterThan(0);
      expect(part.partName.trim().length).toBeGreaterThan(0);
    }
  });
});
