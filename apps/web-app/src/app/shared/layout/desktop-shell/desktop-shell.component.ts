import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/auth.models';

export type DesktopNavKey =
  | 'queue'
  | 'workbench'
  | 'fleet'
  | 'operations'
  | 'approvals'
  | 'catalog'
  | 'analytics'
  | 'billing'
  | 'account';

export interface StaffMobileTab {
  key: DesktopNavKey;
  label: string;
  route: string;
  queryParams?: Record<string, string>;
}

@Component({
  selector: 'app-desktop-shell',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './desktop-shell.component.html',
})
export class DesktopShellComponent {
  private authService: AuthService;
  private router: Router | null;

  @Input() sectionLabel = 'Vistorias e oficina';
  @Input() pageTitle = 'Fila da oficina';
  @Input() activeNav: DesktopNavKey = 'queue';
  @Input() searchPlaceholder = 'Buscar placa, protocolo ou nome...';

  @Output() globalSearch = new EventEmitter<string>();

  isRoleSwitcherOpen = signal<boolean>(false);

  currentUser = computed(() => this.authService?.currentUser?.() ?? null);

  isManager = computed(() => this.currentUser()?.role === 'MANAGER');

  currentRole = computed(() => this.currentUser()?.role || 'MANAGER');

  sessionMode = computed(() => this.authService?.sessionMode?.() ?? 'VITRINE');

  isLiveMode = computed(() => this.authService?.isLiveMode?.() ?? false);

  userFullName = computed(() => this.currentUser()?.fullName || 'Equipe Mottu');

  userInitials = computed(() => {
    const name = this.userFullName();
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  });

  roleLabel = computed(() => {
    return this.isManager() ? 'Gestor da unidade' : 'Mecânico líder';
  });

  accountRoute = computed(() => {
    return this.isManager() ? '/manager/account' : '/mechanic/account';
  });

  mobileTabs = computed<StaffMobileTab[]>(() => {
    if (this.isManager()) {
      return [
        { key: 'approvals', label: 'Usuários', route: '/manager/approvals' },
        { key: 'catalog', label: 'Catálogo', route: '/manager/catalog' },
        { key: 'analytics', label: 'Indicadores', route: '/manager/analytics' },
        { key: 'fleet', label: 'Frota', route: '/mechanic/fleet' },
        { key: 'account', label: 'Conta', route: '/manager/account' },
      ];
    }

    return [
      { key: 'queue', label: 'Fila', route: '/mechanic/queue' },
      { key: 'fleet', label: 'Pátio', route: '/mechanic/fleet' },
      { key: 'operations', label: 'Ordens', route: '/mechanic/operations' },
      { key: 'workbench', label: 'Estoque', route: '/mechanic/operations', queryParams: { tab: 'stock' } },
      { key: 'account', label: 'Conta', route: '/mechanic/account' },
    ];
  });

  constructor(authService?: AuthService, router?: Router) {
    try {
      this.authService = authService ?? inject(AuthService);
    } catch {
      this.authService = authService as AuthService;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
  }

  onSearchInput(value: string): void {
    this.globalSearch.emit(value);
  }

  toggleRoleSwitcher(): void {
    this.isRoleSwitcherOpen.update((v) => !v);
  }

  closeRoleSwitcher(): void {
    this.isRoleSwitcherOpen.set(false);
  }

  onSwitchRole(role: UserRole): void {
    this.authService?.switchRole(role)?.subscribe();
    this.closeRoleSwitcher();
  }

  onSetEnvironment(mode: 'VITRINE' | 'LIVE'): void {
    this.authService?.setSessionMode(mode)?.subscribe();
    this.closeRoleSwitcher();
  }

  onLogout(): void {
    this.authService?.logout();
  }
}

