import { describe, it, expect, beforeEach, vi } from 'vitest';
import { StaffAccountComponent } from './staff-account.component';

describe('StaffAccountComponent (Screen T11 Web - Minha conta corporativa)', () => {
  let comp: StaffAccountComponent;
  let mockProfileService: any;

  beforeEach(() => {
    mockProfileService = {
      getProfile: vi.fn().mockReturnValue({
        userId: 'u-mgr-1',
        role: 'MANAGER',
        fullName: 'Roberto Mendes',
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
      }),
      updateProfile: vi.fn().mockImplementation((patch) => patch),
      changePassword: vi.fn().mockReturnValue({
        success: true,
        message: 'Senha atualizada com sucesso em todos os dispositivos.',
      }),
    };

    comp = new StaffAccountComponent(mockProfileService);
    comp.ngOnInit();
  });

  it('TC-STAFF-1: Initializes corporate profile fields from ProfileService', () => {
    expect(comp.fullName()).toBe('Roberto Mendes');
    expect(comp.employeeCode()).toBe('MOT-SP04-0104');
    expect(comp.roleTitle()).toBe('Gestor da unidade');
  });

  it('TC-STAFF-2: Saves updated corporate profile and displays feedback', () => {
    comp.fullName.set('Roberto Almeida Mendes');
    comp.phone.set('(11) 97777-0000');
    comp.onSaveProfile();

    expect(mockProfileService.updateProfile).toHaveBeenCalledWith(
      expect.objectContaining({
        fullName: 'Roberto Almeida Mendes',
        phone: '(11) 97777-0000',
      })
    );
    expect(comp.successMessage()).toContain('Dados da conta atualizados');
  });

  it('TC-STAFF-3: Copies field to clipboard and sets copiedField signal', () => {
    comp.copyToClipboard('MOT-SP04-0104', 'employeeCode');
    expect(comp.copiedField()).toBe('employeeCode');
  });

  it('TC-STAFF-4: Switches preferred operational unit and saves profile', () => {
    comp.setUnit('Unidade Guarulhos (SP-01)');
    expect(comp.preferredUnit()).toBe('Unidade Guarulhos (SP-01)');
    expect(mockProfileService.updateProfile).toHaveBeenCalledWith(
      expect.objectContaining({
        preferredUnit: 'Unidade Guarulhos (SP-01)',
      })
    );
  });
});

