import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { MobileShellComponent } from './mobile-shell.component';

describe('MobileShellComponent (Uber Clean Fleet Rider App Shell)', () => {
  let comp: MobileShellComponent;
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(() => {
    mockAuthService = {
      currentUser: signal({
        userId: 'u-1',
        role: 'RIDER' as const,
        fullName: 'Carlos Silva',
      }),
      sessionMode: signal('VITRINE'),
      isLiveMode: signal(false),
      userRole: signal('RIDER'),
      switchRole: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
      setSessionMode: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
      logout: vi.fn(),
    };
    mockRouter = {
      navigate: vi.fn(),
    };

    comp = new MobileShellComponent(mockAuthService, mockRouter);
  });

  it('TC-MSHELL-1: Computes user initials cleanly from fullName', () => {
    expect(comp.userInitials()).toBe('CS');
  });

  it('TC-MSHELL-2: Exposes 5 Rider bottom tabs matching prototype (Início, Vistorias, Aluguel, Entregas, Conta)', () => {
    expect(comp.tabs.length).toBe(5);
    expect(comp.tabs.map((t) => t.label)).toEqual([
      'Início',
      'Vistorias',
      'Aluguel',
      'Entregas',
      'Conta',
    ]);
  });

  it('TC-MSHELL-3: Navigates back to configured backRoute on onBack()', () => {
    comp.backRoute = '/rider/garage';
    comp.onBack();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/rider/garage']);
  });

  it('TC-MSHELL-4: Triggers AuthService.logout on logout click', () => {
    comp.onLogout();
    expect(mockAuthService.logout).toHaveBeenCalled();
  });

  it('TC-MSHELL-5: Toggles role switcher and executes role and environment switch on mobile', () => {
    expect(comp.isRoleSwitcherOpen()).toBe(false);
    comp.toggleRoleSwitcher();
    expect(comp.isRoleSwitcherOpen()).toBe(true);

    comp.onSwitchRole('MANAGER');
    expect(mockAuthService.switchRole).toHaveBeenCalledWith('MANAGER');
    expect(comp.isRoleSwitcherOpen()).toBe(false);

    comp.toggleRoleSwitcher();
    comp.onSetEnvironment('LIVE');
    expect(mockAuthService.setSessionMode).toHaveBeenCalledWith('LIVE');
    expect(comp.isRoleSwitcherOpen()).toBe(false);
  });
});

