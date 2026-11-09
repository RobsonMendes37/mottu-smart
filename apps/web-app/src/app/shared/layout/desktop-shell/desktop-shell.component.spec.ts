import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { DesktopShellComponent } from './desktop-shell.component';

describe('DesktopShellComponent (Uber Clean Fleet Web & Mobile Hybrid Shell)', () => {
  let comp: DesktopShellComponent;
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(() => {
    mockAuthService = {
      currentUser: signal({
        userId: 'u-mgr',
        role: 'MANAGER' as const,
        fullName: 'Roberto Mendes',
      }),
      sessionMode: signal('VITRINE'),
      isLiveMode: signal(false),
      userRole: signal('MANAGER'),
      switchRole: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
      setSessionMode: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
      logout: vi.fn(),
    };
    mockRouter = {
      navigate: vi.fn(),
    };

    comp = new DesktopShellComponent(mockAuthService, mockRouter);
  });

  it('TC-DSHELL-1: Computes initials and role label for MANAGER', () => {
    expect(comp.userInitials()).toBe('RM');
    expect(comp.roleLabel()).toBe('Gestor da unidade');
    expect(comp.isManager()).toBe(true);
    expect(comp.accountRoute()).toBe('/manager/account');
  });

  it('TC-DSHELL-2: Adapts navigation and account route for MECHANIC without exposing manager-only links', () => {
    mockAuthService.currentUser.set({
      userId: 'u-mech',
      role: 'MECHANIC' as const,
      fullName: 'Rafael Costa',
    });

    expect(comp.isManager()).toBe(false);
    expect(comp.roleLabel()).toBe('Mecânico líder');
    expect(comp.accountRoute()).toBe('/mechanic/account');
    expect(comp.mobileTabs().map((t) => t.label)).toEqual([
      'Fila',
      'Pátio',
      'Ordens',
      'Estoque',
      'Conta',
    ]);
  });

  it('TC-DSHELL-3: Emits search query and supports logout', () => {
    const spy = vi.spyOn(comp.globalSearch, 'emit');
    comp.onSearchInput('MOT1A23');
    expect(spy).toHaveBeenCalledWith('MOT1A23');

    comp.onLogout();
    expect(mockAuthService.logout).toHaveBeenCalled();
  });

  it('TC-DSHELL-4: Toggle and close role switcher dropdown', () => {
    expect(comp.isRoleSwitcherOpen()).toBe(false);
    comp.toggleRoleSwitcher();
    expect(comp.isRoleSwitcherOpen()).toBe(true);
    comp.closeRoleSwitcher();
    expect(comp.isRoleSwitcherOpen()).toBe(false);
  });

  it('TC-DSHELL-5: onSwitchRole and onSetEnvironment call authService and close switcher', () => {
    comp.isRoleSwitcherOpen.set(true);
    comp.onSwitchRole('MECHANIC');
    expect(mockAuthService.switchRole).toHaveBeenCalledWith('MECHANIC');
    expect(comp.isRoleSwitcherOpen()).toBe(false);

    comp.isRoleSwitcherOpen.set(true);
    comp.onSetEnvironment('LIVE');
    expect(mockAuthService.setSessionMode).toHaveBeenCalledWith('LIVE');
    expect(comp.isRoleSwitcherOpen()).toBe(false);
  });
});

