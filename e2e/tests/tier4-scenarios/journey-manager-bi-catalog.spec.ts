import { test, expect } from '../../src/fixtures/test-context.js';
import { UserRole, AccountStatus, MotorcycleModel } from '../../src/contracts/enums.js';

test.describe('Tier 4: Scenario 6 - Manager Executive BI & Catalog Intelligence Journey', () => {
  test('Complete Journey: Manager approves accounts -> Syncs VTEX catalog -> Edits parts -> Checks BI KPIs & Looker', async ({
    managerClient,
    unauthClient,
    mockServer,
  }) => {
    if (mockServer) mockServer.resetState();

    // 1. New Mechanic registers
    const email = `mecanico_bi_${Date.now()}@test.com`;
    const reg = await unauthClient.register({
      fullName: 'Marcos Mecanico Oficina',
      cpf: '44455566677',
      phone: '11922223333',
      email,
      password: 'Senha@123',
      role: UserRole.MECHANIC,
    });
    expect(reg.status).toBe(201);

    // 2. Manager checks pending list
    const pendingList = await managerClient.getPendingUsers();
    expect(pendingList.status).toBe(200);
    const found = pendingList.data.find((u) => u.id === reg.data.userId);
    expect(found).toBeDefined();

    // 3. Manager approves mechanic
    const approve = await managerClient.approveUser(reg.data.userId, {});
    expect(approve.status).toBe(200);
    expect(approve.data.status).toBe(AccountStatus.ACTIVE);

    // 4. Manager triggers VTEX store catalog synchronization
    const syncRes = await managerClient.syncCatalog();
    expect(syncRes.status).toBe(200);
    expect(syncRes.data.syncedCount).toBeGreaterThan(0);

    // 5. Manager views parts catalog and adjusts pricing
    const parts = await managerClient.getCatalogParts();
    expect(parts.status).toBe(200);
    const partToEdit = parts.data.items[0];
    const update = await managerClient.updateCatalogPart(partToEdit.id, {
      unitPriceBrl: 32.50,
      isNaturalWear: false,
    });
    expect(update.status).toBe(200);
    expect(update.data.unitPriceBrl).toBe(32.50);
    expect(update.data.priceOverridden).toBe(true);

    // 6. Manager checks operational executive dashboard
    const summary = await managerClient.getAnalyticsSummary();
    expect(summary.status).toBe(200);
    expect(summary.data.activeMotorcyclesCount).toBeGreaterThan(0);
    expect(summary.data.autoResolvedPercentage).toBeGreaterThanOrEqual(0);
    expect(summary.data.lookerStudioEmbedUrl).toContain('lookerstudio.google.com');
  });
});
