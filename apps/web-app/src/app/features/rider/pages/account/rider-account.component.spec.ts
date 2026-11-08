import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { RiderAccountComponent } from './rider-account.component';

describe('RiderAccountComponent (Screen T04B - Meus dados do entregador)', () => {
  let comp: RiderAccountComponent;
  let mockProfileService: any;
  let mockRiderStore: any;
  let mockAuthService: any;

  beforeEach(() => {
    mockProfileService = {
      getProfile: vi.fn().mockReturnValue({
        userId: 'u-rider-1',
        role: 'RIDER',
        fullName: 'Carlos Silva',
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
      }),
      updateProfile: vi.fn().mockImplementation((patch) => ({
        fullName: patch.fullName,
        phone: patch.phone,
        email: patch.email,
        pixKey: patch.pixKey,
        preferredUnit: patch.preferredUnit,
      })),
      changePassword: vi.fn().mockReturnValue({
        success: true,
        message: 'Senha atualizada com sucesso em todos os dispositivos.',
      }),
    };

    mockRiderStore = {
      activeMotorcycle: signal({
        id: 'm-1',
        plate: 'MOT1A23',
        model: 'MOTTU_SPORT_110I',
        status: 'RENTED',
        currentOdometerKm: 1035,
        yardName: 'Butantã',
      }),
      garage: signal(null),
      loadGarage: vi.fn().mockResolvedValue(undefined),
    };

    mockAuthService = {
      logout: vi.fn(),
    };

    comp = new RiderAccountComponent(mockProfileService, mockRiderStore, mockAuthService);
    comp.ngOnInit();
  });

  it('TC-T04B-1: Loads rider profile fields and active plate on init', () => {
    expect(comp.fullName()).toBe('Carlos Silva');
    expect(comp.phone()).toBe('(11) 98765-4321');
    expect(comp.activePlate()).toBe('MOT1A23');
  });

  it('TC-T04B-2: Saves edited profile data via ProfileService and shows confirmation', () => {
    comp.fullName.set('Carlos Silva Junior');
    comp.phone.set('(11) 99111-2233');
    comp.onSaveProfile();

    expect(mockProfileService.updateProfile).toHaveBeenCalledWith(
      expect.objectContaining({
        fullName: 'Carlos Silva Junior',
        phone: '(11) 99111-2233',
      })
    );
    expect(comp.feedbackMessage()).toContain('Dados atualizados');
  });

  it('TC-T04B-3: Submits password change and resets password inputs on success', () => {
    comp.currentPassword.set('Mottu@2026!');
    comp.newPassword.set('NovaSenha@2026');
    comp.onChangePassword();

    expect(mockProfileService.changePassword).toHaveBeenCalledWith('Mottu@2026!', 'NovaSenha@2026');
    expect(comp.currentPassword()).toBe('');
    expect(comp.newPassword()).toBe('');
  });
});

