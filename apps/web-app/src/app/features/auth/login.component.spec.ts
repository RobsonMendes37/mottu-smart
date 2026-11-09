import { describe, it, expect, beforeEach, vi } from 'vitest';
import { LoginComponent } from './login.component';
import { of, throwError } from 'rxjs';

describe('LoginComponent (Screen T01)', () => {
  let comp: LoginComponent;
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(() => {
    mockAuthService = {
      login: vi.fn(),
      register: vi.fn(),
      demoLogin: vi.fn(),
      demoLoginMode: vi.fn(),
      navigateForRole: vi.fn(),
      publicDemoMode: { __isSignal: true, set: vi.fn(), value: true },
    };
    mockRouter = { navigate: vi.fn() };

    comp = new (LoginComponent as any)();
    (comp as any).authService = mockAuthService;
    (comp as any).router = mockRouter;
  });

  it('TC-T01-1: Default tab is LOGIN with seed rider credentials populated', () => {
    expect(comp.activeTab()).toBe('LOGIN');
    expect(comp.loginEmail()).toContain('entregador@mottu-ops.local');
  });

  it('TC-T01-2: Switching tab clears errors and changes active tab', () => {
    comp.errorMessage.set('Algum erro');
    comp.setTab('REGISTER');
    expect(comp.activeTab()).toBe('REGISTER');
    expect(comp.errorMessage()).toBeNull();
  });

  it('TC-T01-3: Login receiving 403 triggers pending approval banner', () => {
    mockAuthService.login.mockReturnValue(
      throwError(() => ({
        status: 403,
        title: 'Acesso Não Liberado',
        detail: 'Seu cadastro está aguardando aprovação do Gestor.',
      }))
    );

    comp.onLogin();

    expect(comp.pendingApprovalNotice()).toBe(true);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T01-4: Registration with empty fields sets client error message', () => {
    comp.registerFullName.set('');
    comp.onRegister();
    expect(comp.errorMessage()).toBe('Todos os campos são obrigatórios.');
    expect(mockAuthService.register).not.toHaveBeenCalled();
  });

  it('TC-T01-5: Demo login button triggers authService.demoLogin', () => {
    mockAuthService.demoLogin.mockReturnValue(of({}));
    comp.onDemoLogin('MECHANIC');
    expect(mockAuthService.demoLogin).toHaveBeenCalledWith('MECHANIC');
  });

  it('TC-T01-6: Valid registration sends cleaned CPF/phone and sets success state', () => {
    comp.registerFullName.set('João Silva');
    comp.registerCpf.set('123.456.789-01');
    comp.registerPhone.set('(11) 98765-4321');
    comp.registerEmail.set('joao@silva.com');
    comp.registerPassword.set('Segredo@123');
    comp.registerRole.set('RIDER');

    mockAuthService.register.mockReturnValue(
      of({
        userId: 'uuid-registered-1',
        status: 'PENDING_APPROVAL',
        message: 'Cadastro recebido com sucesso!',
      })
    );

    comp.onRegister();

    expect(mockAuthService.register).toHaveBeenCalledWith({
      fullName: 'João Silva',
      cpf: '12345678901',
      phone: '11987654321',
      email: 'joao@silva.com',
      password: 'Segredo@123',
      role: 'RIDER',
    });
    expect(comp.successMessage()).toBe('Cadastro recebido com sucesso!');
    expect(comp.pendingApprovalNotice()).toBe(true);
    expect(comp.activeTab()).toBe('LOGIN');
  });

  it('TC-T01-7: Login success navigates to role home', () => {
    const loginRes = {
      accessToken: 'valid.token',
      userId: 'user-1',
      role: 'RIDER' as const,
      fullName: 'Entregador Demo',
    };
    mockAuthService.login.mockReturnValue(of(loginRes));

    comp.onLogin();

    expect(comp.isLoading()).toBe(false);
    expect(mockAuthService.navigateForRole).toHaveBeenCalledWith('RIDER');
  });

  it('TC-T01-8: onDemoLoginMode triggers authService.demoLoginMode with mode and role', () => {
    mockAuthService.demoLoginMode.mockReturnValue(of({}));
    comp.onDemoLoginMode('LIVE', 'MANAGER');
    expect(mockAuthService.demoLoginMode).toHaveBeenCalledWith('LIVE', 'MANAGER');
  });

  it('TC-T01-9: setQuickAccessMode switches between VITRINE and LIVE', () => {
    expect(comp.quickAccessMode()).toBe('VITRINE');
    comp.setQuickAccessMode('LIVE');
    expect(comp.quickAccessMode()).toBe('LIVE');
  });

  it('TC-T01-10: onQuickLogin delegates to onDemoLoginMode with current quickAccessMode', () => {
    mockAuthService.demoLoginMode.mockReturnValue(of({}));
    comp.setQuickAccessMode('LIVE');
    comp.onQuickLogin('RIDER');
    expect(mockAuthService.demoLoginMode).toHaveBeenCalledWith('LIVE', 'RIDER');
  });
});
