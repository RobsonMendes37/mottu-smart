import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuthService, SEED_CREDENTIALS } from './auth.service';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

describe('AuthService', () => {
  let service: AuthService;
  let mockHttp: any;
  let mockRouter: any;

  beforeEach(() => {
    localStorage.clear();
    mockHttp = {
      get: vi.fn().mockReturnValue(of({ publicDemoMode: true })),
      post: vi.fn(),
    };
    mockRouter = {
      navigate: vi.fn(),
    };

    service = new (AuthService as any)();
    (service as any).http = mockHttp;
    (service as any).router = mockRouter;
  });

  it('TC-AUTH-1: Initial state is unauthenticated when storage is empty', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.accessToken()).toBeNull();
    expect(service.userRole()).toBeNull();
  });

  it('TC-AUTH-2: Login success saves token and updates signals', () => {
    const loginResponse = {
      accessToken: 'jwt.token.mock',
      userId: 'uuid-123',
      role: 'RIDER',
      fullName: 'Carlos Entregador',
    };
    mockHttp.post.mockReturnValue(of(loginResponse));

    service.login({ email: 'entregador@mottu-ops.local', password: 'Mottu@2026!' })
      .subscribe((res) => {
        expect(res).toEqual(loginResponse);
      });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.accessToken()).toBe('jwt.token.mock');
    expect(service.currentUser()?.fullName).toBe('Carlos Entregador');
    expect(service.isRider()).toBe(true);
    expect(service.isManager()).toBe(false);
  });

  it('TC-AUTH-3: Logout clears signals, storage and navigates to login', () => {
    (service as any).setSession({
      accessToken: 'token',
      userId: 'uuid',
      role: 'MANAGER',
      fullName: 'Gestor',
    });
    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.accessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('TC-AUTH-4: Demo login switches role and navigates to role home', () => {
    const loginResponse = {
      accessToken: 'manager.jwt',
      userId: 'uuid-mgr',
      role: 'MANAGER',
      fullName: 'Gestor Mottu',
    };
    mockHttp.post.mockReturnValue(of(loginResponse));

    service.demoLogin('MANAGER').subscribe();

    expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/login', SEED_CREDENTIALS.MANAGER);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/manager/approvals']);
  });

  it('TC-AUTH-5: Registration forwards payload to register endpoint', () => {
    const regPayload = {
      fullName: 'Novo Entregador',
      cpf: '12345678901',
      phone: '11999990000',
      email: 'novo@email.com',
      password: 'Senha@123',
      role: 'RIDER' as const,
    };
    const regResponse = {
      userId: 'uuid-reg',
      status: 'PENDING_APPROVAL' as const,
      message: 'Cadastro recebido! Aguarde a liberação do Gestor.',
    };
    mockHttp.post.mockReturnValue(of(regResponse));

    service.register(regPayload).subscribe((res) => {
      expect(res).toEqual(regResponse);
    });

    expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/register', regPayload);
  });

  it('TC-AUTH-6: Navigates appropriately for MECHANIC and RIDER roles', () => {
    service.navigateForRole('MECHANIC');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/mechanic/queue']);

    service.navigateForRole('RIDER');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/rider/garage']);
  });

  it('TC-AUTH-8: Navigates MANAGER role to /manager/approvals', () => {
    service.navigateForRole('MANAGER');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/manager/approvals']);
  });

  it('TC-AUTH-7: Handles error response by mapping to ProblemDetail', () => {
    const errorResponse = new HttpErrorResponse({
      error: {
        title: 'Credenciais Inválidas',
        status: 401,
        detail: 'E-mail ou senha incorretos.',
      },
      status: 401,
      statusText: 'Unauthorized',
    });
    mockHttp.post.mockReturnValue(throwError(() => errorResponse));

    service.login({ email: 'err@mottu.local', password: 'bad' }).subscribe({
      next: () => {
        throw new Error('Should not succeed');
      },
      error: (prob) => {
        expect(prob.status).toBe(401);
        expect(prob.title).toBe('Credenciais Inválidas');
        expect(prob.detail).toBe('E-mail ou senha incorretos.');
      },
    });
  });

  it('TC-AUTH-9: demoLoginMode saves mode and logs in with proper credentials', () => {
    const liveRiderResponse = {
      accessToken: 'rider.live.jwt',
      userId: 'uuid-rider-live',
      role: 'RIDER',
      fullName: 'Rafael Entregador (Vida Real)',
    };
    mockHttp.post.mockReturnValue(of(liveRiderResponse));

    service.demoLoginMode('LIVE', 'RIDER').subscribe();

    expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/login', SEED_CREDENTIALS.LIVE.RIDER);
    expect(service.sessionMode()).toBe('LIVE');
    expect(service.isLiveMode()).toBe(true);
    expect(service.modeLabel()).toBe('Operação Real');
  });

  it('TC-AUTH-10: switchRole hot-swaps role preserving active mode', () => {
    const liveMechResponse = {
      accessToken: 'mech.live.jwt',
      userId: 'uuid-mech-live',
      role: 'MECHANIC',
      fullName: 'Diego Mecânico (Vida Real)',
    };
    mockHttp.post.mockReturnValue(of(liveMechResponse));
    service.sessionMode.set('LIVE');

    service.switchRole('MECHANIC').subscribe();

    expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/login', SEED_CREDENTIALS.LIVE.MECHANIC);
  });

  it('TC-AUTH-11: setSessionMode updates environment mode and switches current role', () => {
    const vitrineMgrResponse = {
      accessToken: 'mgr.vitrine.jwt',
      userId: 'uuid-mgr-vitrine',
      role: 'MANAGER',
      fullName: 'Gestor Operações',
    };
    mockHttp.post.mockReturnValue(of(vitrineMgrResponse));
    (service as any).currentUser.set({ userId: 'u', role: 'MANAGER', fullName: 'Gestor' });

    service.setSessionMode('VITRINE').subscribe();

    expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/login', SEED_CREDENTIALS.VITRINE.MANAGER);
    expect(service.sessionMode()).toBe('VITRINE');
    expect(service.isLiveMode()).toBe(false);
  });
});
