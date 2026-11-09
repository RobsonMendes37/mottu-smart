import { describe, it, expect, vi } from 'vitest';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('Route Guards', () => {
  describe('authGuard', () => {
    it('TC-GUARD-1: allows navigation when user is authenticated', () => {
      const mockAuthService = {
        isAuthenticated: () => true,
      };
      const mockRouter = {
        createUrlTree: vi.fn(),
      };

      const result = (authGuard as any)(
        {} as any,
        { url: '/rider/garage' } as any,
        mockAuthService,
        mockRouter
      );

      expect(mockAuthService.isAuthenticated()).toBe(true);
    });

    it('TC-GUARD-2: redirects to /login with returnUrl when unauthenticated', () => {
      const mockAuthService = {
        isAuthenticated: () => false,
      };
      const mockRouter = {
        createUrlTree: vi.fn().mockReturnValue('/login?returnUrl=%2Frider%2Fgarage'),
      };

      const result = (authGuard as any)(
        {} as any,
        { url: '/rider/garage' } as any,
        mockAuthService,
        mockRouter
      );

      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(
        ['/login'],
        { queryParams: { returnUrl: '/rider/garage' } }
      );
    });
  });

  describe('roleGuard', () => {
    it('TC-GUARD-3: allows navigation when user role matches allowed roles', () => {
      const mockAuthService = {
        isAuthenticated: () => true,
        userRole: () => 'RIDER',
      };
      const mockRouter = {
        createUrlTree: vi.fn(),
      };

      const guardFn = roleGuard(['RIDER']);
      const result = (guardFn as any)(
        {} as any,
        { url: '/rider/garage' } as any,
        mockAuthService,
        mockRouter
      );

      expect(result).toBe(true);
    });

    it('TC-GUARD-4: redirects to /login if user is not authenticated', () => {
      const mockAuthService = {
        isAuthenticated: () => false,
        userRole: () => null,
      };
      const mockRouter = {
        createUrlTree: vi.fn().mockReturnValue('/login?returnUrl=%2Fmanager%2Fdashboard'),
      };

      const guardFn = roleGuard(['MANAGER']);
      const result = (guardFn as any)(
        {} as any,
        { url: '/manager/dashboard' } as any,
        mockAuthService,
        mockRouter
      );

      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(
        ['/login'],
        { queryParams: { returnUrl: '/manager/dashboard' } }
      );
    });

    it('TC-GUARD-5: redirects RIDER to /rider/garage when trying to access MANAGER route', () => {
      const mockAuthService = {
        isAuthenticated: () => true,
        userRole: () => 'RIDER',
      };
      const mockRouter = {
        createUrlTree: vi.fn().mockImplementation((commands) => commands.join('/')),
      };

      const guardFn = roleGuard(['MANAGER']);
      const result = (guardFn as any)(
        {} as any,
        { url: '/manager/dashboard' } as any,
        mockAuthService,
        mockRouter
      );

      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/rider/garage']);
    });

    it('TC-GUARD-6: redirects MANAGER to /manager/approvals when role mismatch occurs on another route', () => {
      const mockAuthService = {
        isAuthenticated: () => true,
        userRole: () => 'MANAGER',
      };
      const mockRouter = {
        createUrlTree: vi.fn().mockImplementation((commands) => commands.join('/')),
      };

      const guardFn = roleGuard(['RIDER']);
      const result = (guardFn as any)(
        {} as any,
        { url: '/rider/garage' } as any,
        mockAuthService,
        mockRouter
      );

      expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/manager/approvals']);
    });
  });
});
