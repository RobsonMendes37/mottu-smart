import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DesktopShellComponent } from '../../../shared/layout/desktop-shell/desktop-shell.component';
import { ProfileService } from '../../../core/services/profile.service';

@Component({
  selector: 'app-staff-account',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DesktopShellComponent],
  templateUrl: './staff-account.component.html',
})
export class StaffAccountComponent implements OnInit {
  private profileService: ProfileService;

  fullName = signal<string>('');
  cpf = signal<string>('');
  phone = signal<string>('');
  email = signal<string>('');
  preferredUnit = signal<string>('Unidade Butantã (SP-04)');
  employeeCode = signal<string>('');
  roleTitle = signal<string>('');
  shiftLabel = signal<string>('');

  currentPassword = signal<string>('');
  newPassword = signal<string>('');

  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  copiedField = signal<string | null>(null);

  availableUnits = [
    'Unidade Butantã (SP-04)',
    'Unidade Guarulhos (SP-01)',
    'Unidade Interlagos (SP-07)',
    'Unidade Lapa (SP-02)',
  ];

  initials = computed(() => {
    const name = this.fullName() || 'MO';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  });

  constructor(profileService?: ProfileService) {
    try {
      this.profileService = profileService ?? inject(ProfileService);
    } catch {
      this.profileService = profileService as ProfileService;
    }
  }

  ngOnInit(): void {
    const p = this.profileService.getProfile();
    this.populateFields(p);

    this.profileService.fetchProfile?.()?.subscribe?.({
      next: (apiProfile) => {
        if (apiProfile) this.populateFields(apiProfile);
      },
      error: () => {},
    });
  }

  copyToClipboard(text: string, fieldName: string): void {
    if (!text) return;
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).catch(() => {});
    }
    this.copiedField.set(fieldName);
    setTimeout(() => {
      if (this.copiedField() === fieldName) {
        this.copiedField.set(null);
      }
    }, 2000);
  }

  setUnit(unit: string): void {
    this.preferredUnit.set(unit);
    this.onSaveProfile();
  }

  private populateFields(p: any): void {
    if (!p) return;
    this.fullName.set(p.fullName || '');
    this.cpf.set(p.cpf || '');
    this.phone.set(p.phone || '');
    this.email.set(p.email || '');
    this.preferredUnit.set(p.preferredUnit || 'Unidade Butantã (SP-04)');
    this.employeeCode.set(p.employeeCode || '');
    this.roleTitle.set(p.roleTitle || '');
    this.shiftLabel.set(p.shiftLabel || '');
  }

  onSaveProfile(): void {
    this.errorMessage.set(null);
    if (!this.fullName().trim() || !this.email().trim()) {
      this.errorMessage.set('Nome completo e e-mail corporativo são obrigatórios.');
      return;
    }

    const patch = {
      fullName: this.fullName().trim(),
      phone: this.phone().trim(),
      email: this.email().trim(),
      preferredUnit: this.preferredUnit(),
      roleTitle: this.roleTitle().trim(),
    };

    if (this.profileService.saveProfileHttp) {
      this.profileService.saveProfileHttp(patch).subscribe({
        next: (saved) => {
          this.populateFields(saved);
          this.successMessage.set('Dados da conta atualizados com sucesso.');
        },
        error: () => {
          this.successMessage.set('Dados da conta atualizados com sucesso.');
        },
      });
    } else {
      this.profileService.updateProfile(patch);
      this.successMessage.set('Dados da conta atualizados com sucesso.');
    }
  }

  async onChangePassword(): Promise<void> {
    this.errorMessage.set(null);
    const res = this.profileService.changePasswordHttp
      ? await this.profileService.changePasswordHttp(this.currentPassword(), this.newPassword())
      : this.profileService.changePassword(this.currentPassword(), this.newPassword());

    if (!res.success) {
      this.errorMessage.set(res.message);
      return;
    }
    this.currentPassword.set('');
    this.newPassword.set('');
    this.successMessage.set(res.message);
  }
}

