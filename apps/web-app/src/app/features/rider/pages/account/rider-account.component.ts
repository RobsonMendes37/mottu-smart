import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MobileShellComponent } from '../../../../shared/layout/mobile-shell/mobile-shell.component';
import { ProfileService } from '../../../../core/services/profile.service';
import { RiderStore } from '../../state/rider.store';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-rider-account',
  standalone: true,
  imports: [CommonModule, FormsModule, MobileShellComponent],
  templateUrl: './rider-account.component.html',
})
export class RiderAccountComponent implements OnInit {
  private profileService: ProfileService;
  protected riderStore: RiderStore;
  private authService: AuthService | null;

  fullName = signal<string>('');
  cpf = signal<string>('');
  phone = signal<string>('');
  email = signal<string>('');
  preferredUnit = signal<string>('Unidade Butantã (SP-04)');
  pixKey = signal<string>('');
  cnhStatus = signal<string>('');
  contractCode = signal<string>('CTR-2026-8841');

  currentPassword = signal<string>('');
  newPassword = signal<string>('');

  feedbackMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  readonly availableUnits: string[] = [
    'Unidade Butantã (SP-04)',
    'Unidade Pinheiros (SP-02)',
    'Unidade Guarulhos (SP-07)',
    'Unidade Santo Amaro (SP-09)',
  ];

  activePlate = computed(() => {
    const fn = this.riderStore?.activeMotorcycle;
    const bike = typeof fn === 'function' ? fn() : null;
    return bike?.plate || 'MOT1A23';
  });

  initials = computed(() => {
    const name = this.fullName() || 'CS';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  });

  constructor(
    profileService?: ProfileService,
    riderStore?: RiderStore,
    authService?: AuthService
  ) {
    try {
      this.profileService = profileService ?? inject(ProfileService);
    } catch {
      this.profileService = profileService as ProfileService;
    }
    try {
      this.riderStore = riderStore ?? inject(RiderStore);
    } catch {
      this.riderStore = riderStore as RiderStore;
    }
    try {
      this.authService = authService ?? inject(AuthService, { optional: true });
    } catch {
      this.authService = authService ?? null;
    }
  }

  ngOnInit(): void {
    const profile = this.profileService.getProfile();
    this.fullName.set(profile.fullName);
    this.cpf.set(profile.cpf);
    this.phone.set(profile.phone);
    this.email.set(profile.email);
    this.preferredUnit.set(profile.preferredUnit);
    this.pixKey.set(profile.pixKey);
    this.cnhStatus.set(profile.cnhStatus);
    this.contractCode.set(profile.employeeCode);

    if (this.riderStore?.garage && !this.riderStore.garage()) {
      this.riderStore.loadGarage?.();
    }
  }

  onSaveProfile(): void {
    this.errorMessage.set(null);
    if (!this.fullName().trim() || !this.phone().trim()) {
      this.errorMessage.set('Nome completo e WhatsApp são obrigatórios.');
      return;
    }

    this.profileService.updateProfile({
      fullName: this.fullName().trim(),
      phone: this.phone().trim(),
      email: this.email().trim(),
      preferredUnit: this.preferredUnit(),
      pixKey: this.pixKey().trim(),
    });

    this.feedbackMessage.set('Dados atualizados com sucesso.');
  }

  onChangePassword(): void {
    this.errorMessage.set(null);
    const res = this.profileService.changePassword(this.currentPassword(), this.newPassword());
    if (!res.success) {
      this.errorMessage.set(res.message);
      return;
    }

    this.currentPassword.set('');
    this.newPassword.set('');
    this.feedbackMessage.set(res.message);
  }

  onLogout(): void {
    this.authService?.logout();
  }
}

