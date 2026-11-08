import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { T08ApprovalsComponent } from './t08-approvals.component';
import {
  PendingUserResponseDto,
  AvailableMotorcycleOption,
  ApproveUserResponseDto,
  RejectUserResponseDto,
} from '../../models/manager.models';

describe('T08ApprovalsComponent (Screen T08 - User & Contract Approvals)', () => {
  let comp: T08ApprovalsComponent;
  let mockManagerService: any;

  const mockPendingUsers: PendingUserResponseDto[] = [
    {
      id: 'user-rider-1',
      fullName: 'João Silva',
      cpf: '123.456.789-01',
      phone: '11999998888',
      email: 'joao@rider.com',
      role: 'RIDER',
      createdAt: '2026-11-06T14:30:00Z',
    },
    {
      id: 'user-mechanic-1',
      fullName: 'Carlos Alberto',
      cpf: '987.654.321-09',
      phone: '11988887777',
      email: 'carlos@mechanic.com',
      role: 'MECHANIC',
      createdAt: '2026-11-06T15:00:00Z',
    },
  ];

  const mockAvailableMotorcycles: AvailableMotorcycleOption[] = [
    {
      id: 'moto-1',
      plate: 'MOT2B34',
      model: 'MOTTU_SPORT_110I',
      yardName: 'Pátio Butantã',
    },
    {
      id: 'moto-2',
      plate: 'POP3C45',
      model: 'HONDA_POP_110I',
      yardName: 'Pátio Butantã',
    },
  ];

  beforeEach(() => {
    mockManagerService = {
      getPendingUsers: vi.fn().mockReturnValue(of(mockPendingUsers)),
      getAvailableMotorcycles: vi.fn().mockReturnValue(of(mockAvailableMotorcycles)),
      approveUser: vi.fn().mockReturnValue(
        of<ApproveUserResponseDto>({
          userId: 'user-rider-1',
          status: 'ACTIVE',
          contractId: 'contract-123',
        })
      ),
      rejectUser: vi.fn().mockReturnValue(
        of<RejectUserResponseDto>({
          userId: 'user-mechanic-1',
          status: 'REJECTED',
        })
      ),
    };

    comp = new T08ApprovalsComponent(mockManagerService);
  });

  it('TC-T08-1: ngOnInit loads pending users and available motorcycles', () => {
    comp.ngOnInit();
    expect(mockManagerService.getPendingUsers).toHaveBeenCalled();
    expect(mockManagerService.getAvailableMotorcycles).toHaveBeenCalled();
    expect(comp.pendingUsers().length).toBe(2);
    expect(comp.availableMotorcycles().length).toBe(2);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T08-2: Approving RIDER without selecting a motorcycle sets error and prevents API call', () => {
    comp.ngOnInit();
    const rider = mockPendingUsers[0]; // RIDER

    // No motorcycle selected yet for this rider
    comp.approve(rider);

    expect(comp.errorMessage()).toContain('Selecione uma moto');
    expect(mockManagerService.approveUser).not.toHaveBeenCalled();
  });

  it('TC-T08-3: Approving RIDER with motorcycle selected calls approveUser with motorcycleId and removes user from list', () => {
    comp.ngOnInit();
    const rider = mockPendingUsers[0];

    comp.selectMotorcycle(rider.id, 'moto-1');
    comp.approve(rider);

    expect(mockManagerService.approveUser).toHaveBeenCalledWith('user-rider-1', {
      motorcycleId: 'moto-1',
    });
    expect(comp.successMessage()).toContain('aprovado com sucesso');
    // User should be removed from pending list
    expect(comp.pendingUsers().find((u) => u.id === 'user-rider-1')).toBeUndefined();
  });

  it('TC-T08-4: Approving MECHANIC does not require motorcycle and succeeds directly', () => {
    comp.ngOnInit();
    const mechanic = mockPendingUsers[1]; // MECHANIC

    comp.approve(mechanic);

    expect(mockManagerService.approveUser).toHaveBeenCalledWith('user-mechanic-1');
    expect(comp.successMessage()).toContain('aprovado com sucesso');
    expect(comp.pendingUsers().find((u) => u.id === 'user-mechanic-1')).toBeUndefined();
  });

  it('TC-T08-5: Opening reject modal sets rejectingUser and cleans rejectReason', () => {
    const user = mockPendingUsers[0];
    comp.openRejectModal(user);

    expect(comp.rejectingUser()).toEqual(user);
    expect(comp.rejectReason()).toBe('');
  });

  it('TC-T08-6: Rejecting user with empty or short reason (<5 chars) sets validation error', () => {
    const user = mockPendingUsers[0];
    comp.openRejectModal(user);

    comp.setRejectReason('abc'); // < 5 chars
    comp.confirmReject();

    expect(comp.rejectValidationError()).toContain('pelo menos 5 caracteres');
    expect(mockManagerService.rejectUser).not.toHaveBeenCalled();
  });

  it('TC-T08-7: Confirming reject with valid reason calls rejectUser and removes user from list', () => {
    comp.ngOnInit();
    const user = mockPendingUsers[1];
    comp.openRejectModal(user);

    comp.setRejectReason('CNH não confere com cadastro');
    comp.confirmReject();

    expect(mockManagerService.rejectUser).toHaveBeenCalledWith('user-mechanic-1', {
      reason: 'CNH não confere com cadastro',
    });
    expect(comp.rejectingUser()).toBeNull();
    expect(comp.pendingUsers().find((u) => u.id === 'user-mechanic-1')).toBeUndefined();
  });

  it('TC-T08-8: Handles error when loading pending users fails', () => {
    mockManagerService.getPendingUsers.mockReturnValue(throwError(() => new Error('API Error')));
    comp.loadData();

    expect(comp.errorMessage()).toContain('Erro ao carregar');
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T08-9: Supports inline search and right 400px Slide-over Drawer selection', () => {
    comp.ngOnInit();
    comp.onSearch('Carlos');
    expect(comp.filteredUsers().length).toBe(1);
    expect(comp.filteredUsers()[0].fullName).toBe('Carlos Alberto');

    comp.openUserDrawer(mockPendingUsers[0]);
    expect(comp.drawerUser()?.id).toBe('user-rider-1');

    comp.closeUserDrawer();
    expect(comp.drawerUser()).toBeNull();
  });
});
