import { test, expect } from '../../src/fixtures/test-context.js';
import { UserRole } from '../../src/contracts/enums.js';
import { ContractValidator } from '../../src/contracts/validator.js';

test.describe('Tier 2: Boundary 01 - Auth & Access Boundaries', () => {
  test('TB1.1: Register with empty password is rejected with 400 Bad Request', async ({ unauthClient }) => {
    const res = await unauthClient.register({
      fullName: 'João Sem Senha',
      cpf: '11122233399',
      phone: '11988880000',
      email: `no_pwd_${Date.now()}@test.com`,
      password: '',
      role: UserRole.RIDER,
    });
    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
  });

  test('TB1.2: Register with empty full name is rejected with 400 Bad Request', async ({ unauthClient }) => {
    const res = await unauthClient.register({
      fullName: '',
      cpf: '22233344499',
      phone: '11988880000',
      email: `no_name_${Date.now()}@test.com`,
      password: 'Senha@123',
      role: UserRole.RIDER,
    });
    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
  });

  test('TB1.3: Register attempting role = MANAGER is rejected with 400 Bad Request', async ({ unauthClient }) => {
    const res = await unauthClient.register({
      fullName: 'Invasor Gestor',
      cpf: '33344455599',
      phone: '11988880000',
      email: `fake_mgr_${Date.now()}@test.com`,
      password: 'Senha@123',
      role: UserRole.MANAGER,
    });
    expect(res.status).toBe(400);
    ContractValidator.assertProblemDetail(res.data, 400);
    expect(res.data.detail).toContain('MANAGER');
  });

  test('TB1.4: Login with incorrect password returns 401 Unauthorized', async ({ unauthClient }) => {
    const res = await unauthClient.login({
      email: 'gestor@mottu-ops.local',
      password: 'SenhaTotalmenteErrada!',
    });
    expect(res.status).toBe(401);
    ContractValidator.assertProblemDetail(res.data, 401);
  });

  test('TB1.5: Login with non-existent email returns 401 Unauthorized', async ({ unauthClient }) => {
    const res = await unauthClient.login({
      email: 'inexistente_9999@mottu-ops.local',
      password: 'Mottu@2026!',
    });
    expect(res.status).toBe(401);
    ContractValidator.assertProblemDetail(res.data, 401);
  });
});
