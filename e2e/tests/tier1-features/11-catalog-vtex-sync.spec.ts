import { test, expect } from '../../src/fixtures/test-context.js';
import { MotorcycleModel } from '../../src/contracts/enums.js';

test.describe('Tier 1: Feature 11 - Parts Catalog & VTEX Sync', () => {
  test('TC11.1: Manager triggers VTEX store catalog sync returning 200 OK with syncedCount', async ({ managerClient }) => {
    const res = await managerClient.syncCatalog();
    expect(res.status).toBe(200);
    expect(res.data.syncedCount).toBeGreaterThan(0);
    expect(res.data.message).toContain('sincronizado');
    expect(res.data.syncedAt).toBeTruthy();
  });

  test('TC11.2: Catalog query returns parts list with prices and categories', async ({ managerClient }) => {
    const res = await managerClient.getCatalogParts();
    expect(res.status).toBe(200);
    expect(Array.isArray(res.data.items)).toBe(true);
    expect(res.data.items.length).toBeGreaterThanOrEqual(4);
    const manete = res.data.items.find((p) => p.partName.includes('Manete'));
    expect(manete).toBeDefined();
    expect(manete?.unitPriceBrl).toBeGreaterThan(0);
  });

  test('TC11.3: Manager manually overrides part price setting priceOverridden = true', async ({ managerClient }) => {
    const partsRes = await managerClient.getCatalogParts();
    const targetPart = partsRes.data.items[0];

    const patchRes = await managerClient.updateCatalogPart(targetPart.id, {
      unitPriceBrl: 29.90,
      isNaturalWear: false,
    });

    expect(patchRes.status).toBe(200);
    expect(patchRes.data.unitPriceBrl).toBe(29.90);
    expect(patchRes.data.priceOverridden).toBe(true);
  });

  test('TC11.4: Subsequent catalog sync preserves manually overridden price without overwriting', async ({ managerClient, mockServer }) => {
    const partsRes = await managerClient.getCatalogParts();
    const overriddenPart = partsRes.data.items.find((p) => p.priceOverridden);
    expect(overriddenPart).toBeDefined();

    // Trigger sync again
    await managerClient.syncCatalog();

    // Verify price is preserved
    const partsAfterSync = await managerClient.getCatalogParts();
    const checked = partsAfterSync.data.items.find((p) => p.id === overriddenPart?.id);
    expect(checked?.unitPriceBrl).toBe(overriddenPart?.unitPriceBrl);
    expect(checked?.priceOverridden).toBe(true);
  });

  test('TC11.5: Non-manager cannot trigger catalog sync or edit prices', async ({ riderClient }) => {
    const syncRes = await riderClient.syncCatalog();
    expect(syncRes.status).toBe(403);
  });
});
