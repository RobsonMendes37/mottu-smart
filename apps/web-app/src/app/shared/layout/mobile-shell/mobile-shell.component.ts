import { Component, Input, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/auth.models';

export type RiderTabKey = 'home' | 'inspections' | 'rental' | 'deliveries' | 'account';

export interface MobileTabItem {
  key: RiderTabKey;
  label: string;
  route: string;
  queryParams?: Record<string, string>;
  isMock?: boolean;
}

@Component({
  selector: 'app-mobile-shell',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mobile-shell.component.html',
})
export class MobileShellComponent {
  private authService: AuthService;
  private router: Router | null;

  @Input() title = 'Mottu';
  @Input() subtitle = 'Unidade Butantã';
  @Input() activeTab: RiderTabKey = 'home';
  @Input() showBack = false;
  @Input() backRoute = '/rider/garage';
  @Input() showBottomNav = true;
  @Input() badgeText = '';

  isRoleSwitcherOpen = signal<boolean>(false);

  sessionMode = computed(() => this.authService?.sessionMode?.() ?? 'VITRINE');
  isLiveMode = computed(() => this.authService?.isLiveMode?.() ?? false);
  currentRole = computed(() => this.authService?.userRole?.() ?? 'RIDER');

  readonly tabs: MobileTabItem[] = [
    { key: 'home', label: 'Início', route: '/rider/garage' },
    { key: 'inspections', label: 'Vistorias', route: '/rider/inspections/new' },
    { key: 'rental', label: 'Aluguel', route: '/rider/rental', isMock: true },
    { key: 'deliveries', label: 'Entregas', route: '/rider/rental', queryParams: { tab: 'deliveries' }, isMock: true },
    { key: 'account', label: 'Conta', route: '/rider/account' },
  ];

  userInitials = computed(() => {
    const name = this.authService?.currentUser?.()?.fullName || 'Carlos Silva';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
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

  onBack(): void {
    this.router?.navigate([this.backRoute]);
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

