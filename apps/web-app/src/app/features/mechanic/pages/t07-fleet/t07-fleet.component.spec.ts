import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { T07FleetComponent } from './t07-fleet.component';
import {
  FleetMotorcycleResponse,
  MotorcycleDetailsResponse,
  PageResponse,
  YardResponse,
} from '../../models/mechanic.models';

describe('T07FleetComponent (Screen T07 - Yard Fleet Management Table)', () => {
  let comp: T07FleetComponent;
  let mockFleetService: any;

  const mockYards: YardResponse[] = [
    {
      id: 'yard-1',
      name: 'Butantã',
      city: 'São Paulo',
      address: 'Av. Corifeu de Azevedo Marques, 1000',
      active: true,
    },
    {
      id: 'yard-2',
      name: 'Guarulhos',
      city: 'Guarulhos',
      address: 'Rua das Oficinas, 200',
      active: true,
    },
  ];

  const mockMotorcycles: FleetMotorcycleResponse[] = [
    {
      id: 'moto-1',
      plate: 'MOT1A23',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      yardName: 'Butantã',
      status: 'RENTED',
      currentOdometerKm: 1035,
      activeRiderName: 'Carlos Silva',
    },
    {
      id: 'moto-2',
      plate: 'MOT2B34',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      yardName: 'Butantã',
      status: 'AVAILABLE',
      currentOdometerKm: 0,
      activeRiderName: null,
    },
    {
      id: 'moto-3',
      plate: 'POP3C45',
      model: 'HONDA_POP_110I',
      yardId: 'yard-2',
      yardName: 'Guarulhos',
      status: 'IN_MAINTENANCE',
      currentOdometerKm: 8420,
      activeRiderName: null,
    },
  ];

  const mockPageResponse: PageResponse<FleetMotorcycleResponse> = {
    items: mockMotorcycles,
    page: 0,
    size: 20,
    totalElements: 3,
    totalPages: 1,
  };

  beforeEach(() => {
    mockFleetService = {
      listMotorcycles: vi.fn().mockReturnValue(of(mockPageResponse)),
      getYards: vi.fn().mockReturnValue(of(mockYards)),
      updateStatus: vi.fn(),
      registerMotorcycle: vi.fn(),
    };

    comp = new T07FleetComponent(mockFleetService);
  });

  it('TC-T07-1: ngOnInit loads yards and motorcycles', () => {
    comp.ngOnInit();
    expect(mockFleetService.getYards).toHaveBeenCalled();
    expect(mockFleetService.listMotorcycles).toHaveBeenCalled();
    expect(comp.motorcycles().length).toBe(3);
    expect(comp.yards().length).toBe(2);
    expect(comp.totalElements()).toBe(3);
  });

  it('TC-T07-2: Filters update query parameters and trigger reload', () => {
    comp.setStatusFilter('AVAILABLE');
    expect(comp.statusFilter()).toBe('AVAILABLE');
    expect(mockFleetService.listMotorcycles).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'AVAILABLE' }),
      0,
      20
    );

    comp.onPlateFilter('MOT1');
    expect(comp.plateFilter()).toBe('MOT1');
    expect(mockFleetService.listMotorcycles).toHaveBeenCalledWith(
      expect.objectContaining({ plate: 'MOT1' }),
      0,
      20
    );

    comp.onYardFilter('yard-2');
    expect(comp.yardFilter()).toBe('yard-2');
    expect(mockFleetService.listMotorcycles).toHaveBeenCalledWith(
      expect.objectContaining({ yardId: 'yard-2' }),
      0,
      20
    );
  });

  it('TC-T07-3: quickToggleStatus calls fleetService.updateStatus and refreshes list', () => {
    const updatedDetails: MotorcycleDetailsResponse = {
      id: 'moto-2',
      plate: 'MOT2B34',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      yardName: 'Butantã',
      status: 'IN_MAINTENANCE',
      currentOdometerKm: 0,
      activeRiderName: null,
      createdAt: '2026-11-07T08:00:00Z',
    };
    mockFleetService.updateStatus.mockReturnValue(of(updatedDetails));

    comp.quickToggleStatus('moto-2', 'IN_MAINTENANCE');

    expect(mockFleetService.updateStatus).toHaveBeenCalledWith('moto-2', {
      status: 'IN_MAINTENANCE',
    });
    expect(mockFleetService.listMotorcycles).toHaveBeenCalled();
    expect(comp.successMessage()).toContain('Status atualizado');
  });

  it('TC-T07-4: Register motorcycle validates required plate and yard', () => {
    comp.openCreateModal();
    expect(comp.isModalOpen()).toBe(true);

    comp.newPlate.set('');
    comp.submitCreateMotorcycle();
    expect(comp.modalError()).toContain('Placa é obrigatória');
    expect(mockFleetService.registerMotorcycle).not.toHaveBeenCalled();

    comp.newPlate.set('ABC1234');
    comp.newYardId.set('');
    comp.submitCreateMotorcycle();
    expect(comp.modalError()).toContain('Selecione um galpão');
    expect(mockFleetService.registerMotorcycle).not.toHaveBeenCalled();
  });

  it('TC-T07-5: Register motorcycle submits valid command, closes modal and reloads fleet', () => {
    comp.openCreateModal();
    comp.newPlate.set('mot-9z99');
    comp.newModel.set('MOTTU_SPORT_110I');
    comp.newYardId.set('yard-1');
    comp.newOdometer.set(10);

    const createdDetails: MotorcycleDetailsResponse = {
      id: 'new-moto-id',
      plate: 'MOT9Z99',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      yardName: 'Butantã',
      status: 'AVAILABLE',
      currentOdometerKm: 10,
      activeRiderName: null,
      createdAt: '2026-11-07T08:00:00Z',
    };
    mockFleetService.registerMotorcycle.mockReturnValue(of(createdDetails));

    comp.submitCreateMotorcycle();

    expect(mockFleetService.registerMotorcycle).toHaveBeenCalledWith({
      plate: 'MOT9Z99',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      currentOdometerKm: 10,
    });
    expect(comp.isModalOpen()).toBe(false);
    expect(comp.successMessage()).toContain('Moto MOT9Z99 cadastrada com sucesso!');
  });

  it('TC-T07-6: getStatusBadge adheres strictly to Purple Ban', () => {
    const available = comp.getStatusBadge('AVAILABLE');
    expect(available.colorClass).toContain('text-[#00E676]');
    expect(available.colorClass).not.toMatch(/purple|violet|indigo/);

    const maintenance = comp.getStatusBadge('IN_MAINTENANCE');
    expect(maintenance.colorClass).toContain('text-[#FFB300]');
    expect(maintenance.colorClass).not.toMatch(/purple|violet|indigo/);

    const unavailable = comp.getStatusBadge('UNAVAILABLE');
    expect(unavailable.colorClass).toContain('text-[#FF4D4F]');
    expect(unavailable.colorClass).not.toMatch(/purple|violet|indigo/);
  });
});
