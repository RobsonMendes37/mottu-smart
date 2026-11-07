import { describe, it, expect, vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { of, throwError } from 'rxjs';

describe('authInterceptor', () => {
  it('TC-AUTH-INT-1: Attaches Bearer token to protected requests when token is available', () => {
    let capturedReq: HttpRequest<any> | null = null;
    const nextFn = vi.fn().mockImplementation((req) => {
      capturedReq = req;
      return of({});
    });

    const mockAuthService = {
      accessToken: () => 'valid-jwt-token-xyz',
      logout: vi.fn(),
    };

    const req = new HttpRequest('GET', '/api/v1/rider/garage');
    (authInterceptor as any)(req, nextFn, mockAuthService);

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.get('Authorization')).toBe('Bearer valid-jwt-token-xyz'); // nosec
  });

  it('TC-AUTH-INT-2: Does not attach Bearer token to auth public endpoints', () => {
    let capturedReq: HttpRequest<any> | null = null;
    const nextFn = vi.fn().mockImplementation((req) => {
      capturedReq = req;
      return of({});
    });

    const mockAuthService = {
      accessToken: () => 'valid-jwt-token-xyz',
      logout: vi.fn(),
    };

    const req = new HttpRequest('POST', '/api/v1/auth/login', {});
    (authInterceptor as any)(req, nextFn, mockAuthService);

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.has('Authorization')).toBe(false);
  });

  it('TC-AUTH-INT-3: Calls authService.logout on 401 error from protected endpoint', () => {
    const mockAuthService = {
      accessToken: () => 'expired-jwt',
      logout: vi.fn(),
    };

    const nextFn = vi.fn().mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }))
    );

    const req = new HttpRequest('GET', '/api/v1/fleet/motorcycles');
    (authInterceptor as any)(req, nextFn, mockAuthService).subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
      },
    });

    expect(mockAuthService.logout).toHaveBeenCalled();
  });
});
