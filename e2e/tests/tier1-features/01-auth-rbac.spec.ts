import { test, expect } from '../../src/fixtures/test-context.js';
import { UserRole, AccountStatus } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';
import { SEED_USERS } from '../../src/fixtures/seed-data.js';

test.describe('Tier 1: Feature 01 - Authentication & RBAC', () => {
  test('TC1.1: Register new RIDER with valid data creates user with PENDING_APPROVAL', async ({ unauthClient }) => {
    const res = await unauthClient.register({
      fullName: 'Rider Novo Teste',
      cpf: '12345678909',
      phone: '11988887777',
      email: `rider_${Date.now()}@test.com`,
      password: 'Senha@123',
      role: UserRole.RIDER,
    });

    expect(res.status).toBe(201);
    expect(res.data.status).toBe(AccountStatus.PENDING_APPROVAL);
    expect(ContractValidator.isUUID(res.data.userId)).toBe(true);
    expect(res.data.message).toContain('Aguarde a liberação do Gestor');
  });

  test('TC1.2: Register new MECHANIC creates user with PENDING_APPROVAL', async ({ unauthClient }) => {
    const res = await unauthClient.register({
      fullName: 'Mecanico Oficina',
      cpf: '98765432100',
      phone: '11977776666',
      email: `mecanico_${Date.now()}@test.com`,
      password: 'Senha@123',
      role: UserRole.MECHANIC,
    });

    expect(res.status).toBe(201);
    expect(res.data.status).toBe(AccountStatus.PENDING_APPROVAL);
  });

  test('TC1.3: Manager login with valid credentials returns JWT token and ACTIVE role', async ({ unauthClient }) => {
    const res = await unauthClient.login({
      email: SEED_USERS.MANAGER.email,
      password: SEED_USERS.MANAGER.password,
    });

    expect(res.status).toBe(200);
    expect(res.data.accessToken).toBeTruthy();
    expect(res.data.role).toBe(UserRole.MANAGER);
    expect(res.data.fullName).toBe(SEED_USERS.MANAGER.fullName);
  });

  test('TC1.4: Login with PENDING_APPROVAL user returns 403 Forbidden with RFC 7807', async ({ unauthClient }) => {
    const email = `pending_${Date.now()}@test.com`;
    await unauthClient.register({
      fullName: 'Usuario Pendente',
      cpf: '11122233344',
      phone: '11955554444',
      email,
      password: 'Senha@123',
      role: UserRole.RIDER,
    });

    const loginRes = await unauthClient.login({
      email,
      password: 'Senha@123',
    });

    expect(loginRes.status).toBe(403);
    ContractValidator.assertProblemDetail(loginRes.data, 403);
    expect(loginRes.data.detail).toContain('aguardando aprovação');
  });

  test('TC1.5: Query public config returns publicDemoMode boolean', async ({ unauthClient }) => {
    const res = await unauthClient.getPublicConfig();
    expect(res.status).toBe(200);
    expect(typeof res.data.publicDemoMode).toBe('boolean');
  });

  test('TC1.6: RBAC isolation: RIDER token cannot access Manager routes', async ({ riderClient }) => {
    const res = await riderClient.getPendingUsers();
    expect(res.status).toBe(403);
    ContractValidator.assertProblemDetail(res.data, 403);
  });
});
