import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { FleetService } from './fleet.service';
import {
  FleetMotorcycleResponse,
  MotorcycleDetailsResponse,
  PageResponse,
  YardResponse,
} from '../models/mechanic.models';

describe('FleetService (T07 Fleet & Yard API Client)', () => {
  let service: FleetService;
  let mockHttp: any;

  beforeEach(() => {
    mockHttp = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
    };
    service = new FleetService(mockHttp);
  });

  it('TC-FS-1: listMotorcycles calls /api/v1/fleet/motorcycles with filter params', () => {
    const mockPage: PageResponse<FleetMotorcycleResponse> = {
      items: [
        {
          id: 'moto-1',
          plate: 'MOT1A23',
          model: 'MOTTU_SPORT_110I',
          yardId: 'yard-1',
          yardName: 'Butantã',
          status: 'AVAILABLE',
          currentOdometerKm: 1035,
          activeRiderName: null,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    };

    mockHttp.get.mockReturnValue(of(mockPage));

    let result: PageResponse<FleetMotorcycleResponse> | undefined;
    service
      .listMotorcycles(
        { status: 'AVAILABLE', plate: 'MOT1A23', yardId: 'yard-1', model: 'MOTTU_SPORT_110I' },
        0,
        20
      )
      .subscribe((res) => {
        result = res;
      });

    expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/fleet/motorcycles', {
      params: expect.objectContaining({
        status: 'AVAILABLE',
        plate: 'MOT1A23',
        yardId: 'yard-1',
        model: 'MOTTU_SPORT_110I',
        page: '0',
        size: '20',
      }),
    });
    expect(result?.items.length).toBe(1);
    expect(result?.items[0].plate).toBe('MOT1A23');
  });

  it('TC-FS-2: registerMotorcycle posts CreateMotorcycleCommand to /api/v1/fleet/motorcycles', () => {
    const command = {
      plate: 'MOT9Z99',
      model: 'MOTTU_SPORT_110I' as const,
      yardId: 'yard-1',
      currentOdometerKm: 0,
    };

    const mockDetails: MotorcycleDetailsResponse = {
      id: 'new-moto-id',
      plate: 'MOT9Z99',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      yardName: 'Butantã',
      status: 'AVAILABLE',
      currentOdometerKm: 0,
      activeRiderName: null,
      createdAt: '2026-11-07T08:00:00Z',
    };

    mockHttp.post.mockReturnValue(of(mockDetails));

    let result: MotorcycleDetailsResponse | undefined;
    service.registerMotorcycle(command).subscribe((res) => {
      result = res;
    });

    expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/fleet/motorcycles', command);
    expect(result?.id).toBe('new-moto-id');
    expect(result?.plate).toBe('MOT9Z99');
  });

  it('TC-FS-3: updateStatus patches motorcycle status', () => {
    const mockDetails: MotorcycleDetailsResponse = {
      id: 'moto-1',
      plate: 'MOT1A23',
      model: 'MOTTU_SPORT_110I',
      yardId: 'yard-1',
      yardName: 'Butantã',
      status: 'IN_MAINTENANCE',
      currentOdometerKm: 1035,
      activeRiderName: null,
      createdAt: '2026-11-07T08:00:00Z',
    };

    mockHttp.patch.mockReturnValue(of(mockDetails));

    let result: MotorcycleDetailsResponse | undefined;
    service.updateStatus('moto-1', { status: 'IN_MAINTENANCE' }).subscribe((res) => {
      result = res;
    });

    expect(mockHttp.patch).toHaveBeenCalledWith('/api/v1/fleet/motorcycles/moto-1/status', {
      status: 'IN_MAINTENANCE',
    });
    expect(result?.status).toBe('IN_MAINTENANCE');
  });

  it('TC-FS-4: getYards fetches yards from /api/v1/yards', () => {
    const mockYards: YardResponse[] = [
      {
        id: 'yard-1',
        name: 'Butantã',
        city: 'São Paulo',
        address: 'Av. Corifeu de Azevedo Marques, 1000',
        active: true,
      },
    ];

    mockHttp.get.mockReturnValue(of(mockYards));

    let result: YardResponse[] | undefined;
    service.getYards().subscribe((res) => {
      result = res;
    });

    expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/yards');
    expect(result?.length).toBe(1);
    expect(result?.[0].name).toBe('Butantã');
  });
});
