import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { AuthUser, UserRole } from '../models/auth.models';

export interface UserProfileDetails {
  userId: string;
  role: UserRole;
  fullName: string;
  cpf: string;
  phone: string;
  email: string;
  preferredUnit: string;
  pixKey: string;
  cnhStatus: string;
  employeeCode: string;
  roleTitle: string;
  shiftLabel: string;
  updatedAt: string;
}

const STORAGE_PREFIX = 'mottu_profile_';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private authService: AuthService;

  constructor(authService?: AuthService) {
    try {
      this.authService = authService ?? inject(AuthService);
    } catch {
      this.authService = authService as AuthService;
    }
  }

  getProfile(): UserProfileDetails {
    const user: AuthUser | null = this.authService?.currentUser?.() ?? null;
    const userId = user?.userId || 'guest';
    const role: UserRole = user?.role || 'RIDER';
    const fullName = user?.fullName || 'Carlos Silva';

    const defaults = this.buildDefaults(userId, role, fullName);

    try {
      const raw = localStorage.getItem(`${STORAGE_PREFIX}${userId}`);
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<UserProfileDetails>;
        return {
          ...defaults,
          ...parsed,
          userId,
          role,
        };
      }
    } catch {
      // Fallback to defaults if storage is unavailable
    }

    return defaults;
  }

  updateProfile(patch: Partial<UserProfileDetails>): UserProfileDetails {
    const current = this.getProfile();
    const updated: UserProfileDetails = {
      ...current,
      ...patch,
      userId: current.userId,
      role: current.role,
      updatedAt: new Date().toISOString(),
    };

    try {
      localStorage.setItem(`${STORAGE_PREFIX}${current.userId}`, JSON.stringify(updated));
    } catch {
      // Ignore quota errors
    }

    if (patch.fullName && patch.fullName.trim().length > 0 && this.authService?.currentUser) {
      const authUser = this.authService.currentUser();
      if (authUser) {
        this.authService.currentUser.set({
          ...authUser,
          fullName: patch.fullName.trim(),
        });
      }
    }

    return updated;
  }

  changePassword(currentPassword: string, newPassword: string): { success: boolean; message: string } {
    if (!currentPassword || currentPassword.trim().length === 0) {
      return { success: false, message: 'Informe sua senha atual para confirmar a alteração.' };
    }
    if (!newPassword || newPassword.trim().length < 8) {
      return { success: false, message: 'A nova senha deve ter no mínimo 8 caracteres.' };
    }
    return {
      success: true,
      message: 'Senha atualizada com sucesso em todos os dispositivos.',
    };
  }

  private buildDefaults(userId: string, role: UserRole, fullName: string): UserProfileDetails {
    if (role === 'MANAGER') {
      return {
        userId,
        role,
        fullName,
        cpf: '284.***.***-10',
        phone: '(11) 97654-3210',
        email: 'gestor@mottu-ops.local',
        preferredUnit: 'Unidade Butantã (SP-04)',
        pixKey: 'gestor@mottu-ops.local',
        cnhStatus: 'Categoria AB • Ativa',
        employeeCode: 'MOT-SP04-0104',
        roleTitle: 'Gestor da unidade',
        shiftLabel: 'Comercial • 08h às 18h',
        updatedAt: '2026-11-07T09:00:00Z',
      };
    }

    if (role === 'MECHANIC') {
      return {
        userId,
        role,
        fullName,
        cpf: '391.***.***-44',
        phone: '(11) 98412-5509',
        email: 'mecanico@mottu-ops.local',
        preferredUnit: 'Unidade Butantã (SP-04)',
        pixKey: 'mecanico@mottu-ops.local',
        cnhStatus: 'Categoria A • Ativa',
        employeeCode: 'MOT-SP04-0892',
        roleTitle: 'Mecânico líder — Baia 02',
        shiftLabel: 'Manhã e tarde • 07h às 16h',
        updatedAt: '2026-11-07T09:00:00Z',
      };
    }

    return {
      userId,
      role,
      fullName,
      cpf: '418.***.***-09',
      phone: '(11) 98765-4321',
      email: 'entregador@mottu-ops.local',
      preferredUnit: 'Unidade Butantã (SP-04)',
      pixKey: 'entregador@mottu-ops.local',
      cnhStatus: 'Categoria A • Válida até 08/2029',
      employeeCode: 'CTR-2026-8841',
      roleTitle: 'Entregador parceiro',
      shiftLabel: 'Plano Minha Mottu Semanal',
      updatedAt: '2026-11-07T09:00:00Z',
    };
  }
}
