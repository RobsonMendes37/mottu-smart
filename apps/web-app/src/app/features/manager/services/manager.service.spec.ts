import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { ManagerService } from './manager.service';
import {
  PendingUserResponseDto,
  ApproveUserResponseDto,
  RejectUserResponseDto,
  CatalogPartResponseDto,
  PageResponse,
  SyncCatalogResponseDto,
  ManagerSettingsDto,
  AnalyticsSummaryResponseDto,
} from '../models/manager.models';

describe('ManagerService (API Client for T08, T09, T10)', () => {
  let service: ManagerService;
  let mockHttp: any;

  beforeEach(() => {
    mockHttp = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
    };
    service = new ManagerService(mockHttp);
  });

  it('TC-MS-01: should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getPendingUsers', () => {
    it('TC-MS-02: should issue GET to /api/v1/manager/users/pending', () => {
      const mockUsers: PendingUserResponseDto[] = [
        {
          id: 'user-1',
          fullName: 'João Silva',
          cpf: '12345678901',
          phone: '11999999999',
          email: 'joao@test.com',
          role: 'RIDER',
          createdAt: '2026-11-01T10:00:00Z',
        },
      ];
      mockHttp.get.mockReturnValue(of(mockUsers));

      let result: PendingUserResponseDto[] | undefined;
      service.getPendingUsers().subscribe((users) => {
        result = users;
      });

      expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/manager/users/pending');
      expect(result).toEqual(mockUsers);
      expect(result?.length).toBe(1);
      expect(result?.[0].role).toBe('RIDER');
    });
  });

  describe('approveUser', () => {
    it('TC-MS-03: should issue POST to /api/v1/manager/users/:id/approve with motorcycleId for rider', () => {
      const mockResponse: ApproveUserResponseDto = {
        userId: 'user-1',
        status: 'ACTIVE',
        contractId: 'contract-1',
      };
      mockHttp.post.mockReturnValue(of(mockResponse));

      let result: ApproveUserResponseDto | undefined;
      service.approveUser('user-1', { motorcycleId: 'moto-1' }).subscribe((res) => {
        result = res;
      });

      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/manager/users/user-1/approve', {
        motorcycleId: 'moto-1',
      });
      expect(result).toEqual(mockResponse);
      expect(result?.status).toBe('ACTIVE');
    });

    it('TC-MS-04: should issue POST to /api/v1/manager/users/:id/approve with empty object if request omitted', () => {
      const mockResponse: ApproveUserResponseDto = {
        userId: 'user-2',
        status: 'ACTIVE',
        contractId: null,
      };
      mockHttp.post.mockReturnValue(of(mockResponse));

      let result: ApproveUserResponseDto | undefined;
      service.approveUser('user-2').subscribe((res) => {
        result = res;
      });

      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/manager/users/user-2/approve', {});
      expect(result?.status).toBe('ACTIVE');
      expect(result?.contractId).toBeNull();
    });
  });

  describe('rejectUser', () => {
    it('TC-MS-05: should issue POST to /api/v1/manager/users/:id/reject with reason', () => {
      const mockResponse: RejectUserResponseDto = {
        userId: 'user-1',
        status: 'REJECTED',
      };
      mockHttp.post.mockReturnValue(of(mockResponse));

      let result: RejectUserResponseDto | undefined;
      service.rejectUser('user-1', { reason: 'Documento CNH vencido ou ilegível' }).subscribe((res) => {
        result = res;
      });

      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/manager/users/user-1/reject', {
        reason: 'Documento CNH vencido ou ilegível',
      });
      expect(result).toEqual(mockResponse);
      expect(result?.status).toBe('REJECTED');
    });
  });

  describe('getParts', () => {
    it('TC-MS-06: should issue GET to /api/v1/manager/catalog/parts with query parameters', () => {
      const mockPage: PageResponse<CatalogPartResponseDto> = {
        items: [
          {
            id: 'part-1',
            vtexSku: 'SKU-1001',
            motorcycleModel: 'MOTTU_SPORT_110I',
            partName: 'Manete de Freio Direito',
            category: 'FREIOS',
            unitPriceBrl: 25.0,
            isNaturalWear: false,
            priceOverridden: false,
            imageUrl: null,
            updatedAt: '2026-11-01T10:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      };
      mockHttp.get.mockReturnValue(of(mockPage));

      let result: PageResponse<CatalogPartResponseDto> | undefined;
      service.getParts('MOTTU_SPORT_110I', 'manete', 0, 20).subscribe((res) => {
        result = res;
      });

      expect(mockHttp.get).toHaveBeenCalledWith(
        '/api/v1/manager/catalog/parts',
        expect.objectContaining({
          params: expect.anything(),
        })
      );
      expect(result?.items.length).toBe(1);
      expect(result?.items[0].partName).toContain('Manete');
    });
  });

  describe('syncMottuStore', () => {
    it('TC-MS-07: should issue POST to /api/v1/manager/catalog/sync-mottu-store', () => {
      const mockSync: SyncCatalogResponseDto = {
        syncedCount: 42,
        usedFallback: false,
        syncedAt: '2026-11-07T10:00:00Z',
        message: 'Catálogo sincronizado com sucesso diretamente da loja.mottu.com.br',
      };
      mockHttp.post.mockReturnValue(of(mockSync));

      let result: SyncCatalogResponseDto | undefined;
      service.syncMottuStore().subscribe((res) => {
        result = res;
      });

      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/manager/catalog/sync-mottu-store', {});
      expect(result?.syncedCount).toBe(42);
      expect(result?.usedFallback).toBe(false);
    });
  });

  describe('updatePart', () => {
    it('TC-MS-08: should issue PATCH to /api/v1/manager/catalog/parts/:id', () => {
      const mockUpdated: CatalogPartResponseDto = {
        id: 'part-1',
        vtexSku: 'SKU-1001',
        motorcycleModel: 'MOTTU_SPORT_110I',
        partName: 'Manete de Freio Direito',
        category: 'FREIOS',
        unitPriceBrl: 28.5,
        isNaturalWear: true,
        priceOverridden: true,
        imageUrl: null,
        updatedAt: '2026-11-07T10:05:00Z',
      };
      mockHttp.patch.mockReturnValue(of(mockUpdated));

      let result: CatalogPartResponseDto | undefined;
      service
        .updatePart('part-1', { unitPriceBrl: 28.5, isNaturalWear: true })
        .subscribe((res) => {
          result = res;
        });

      expect(mockHttp.patch).toHaveBeenCalledWith('/api/v1/manager/catalog/parts/part-1', {
        unitPriceBrl: 28.5,
        isNaturalWear: true,
      });
      expect(result?.unitPriceBrl).toBe(28.5);
      expect(result?.priceOverridden).toBe(true);
    });
  });

  describe('getSettings & updateSettings', () => {
    it('TC-MS-09: should GET and PATCH /api/v1/manager/settings', () => {
      const mockSettings: ManagerSettingsDto = { standardLaborFeeBrl: 35.0 };
      mockHttp.get.mockReturnValue(of(mockSettings));
      mockHttp.patch.mockReturnValue(of({ standardLaborFeeBrl: 40.0 }));

      let getResult: ManagerSettingsDto | undefined;
      service.getSettings().subscribe((s) => {
        getResult = s;
      });
      expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/manager/settings');
      expect(getResult?.standardLaborFeeBrl).toBe(35.0);

      let patchResult: ManagerSettingsDto | undefined;
      service.updateSettings({ standardLaborFeeBrl: 40.0 }).subscribe((s) => {
        patchResult = s;
      });
      expect(mockHttp.patch).toHaveBeenCalledWith('/api/v1/manager/settings', {
        standardLaborFeeBrl: 40.0,
      });
      expect(patchResult?.standardLaborFeeBrl).toBe(40.0);
    });
  });

  describe('getAnalyticsSummary', () => {
    it('TC-MS-10: should issue GET to /api/v1/manager/analytics/summary', () => {
      const mockSummary: AnalyticsSummaryResponseDto = {
        activeMotorcyclesCount: 124,
        autoResolvedPercentage: 78.5,
        currentHumanQueueSize: 4,
        estimatedMechanicHoursSaved: 36.2,
        lookerStudioEmbedUrl: 'https://lookerstudio.google.com/embed/reporting/test',
      };
      mockHttp.get.mockReturnValue(of(mockSummary));

      let result: AnalyticsSummaryResponseDto | undefined;
      service.getAnalyticsSummary().subscribe((res) => {
        result = res;
      });

      expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/manager/analytics/summary');
      expect(result?.activeMotorcyclesCount).toBe(124);
      expect(result?.autoResolvedPercentage).toBe(78.5);
    });
  });

  describe('getAvailableMotorcycles', () => {
    it('TC-MS-11: should issue GET to /api/v1/fleet/motorcycles with status=AVAILABLE', () => {
      const mockFleet = {
        items: [
          {
            id: 'moto-1',
            plate: 'MOT2B34',
            model: 'MOTTU_SPORT_110I' as const,
            yardId: 'yard-1',
            yardName: 'Pátio Butantã',
            status: 'AVAILABLE' as const,
            currentOdometerKm: 0,
            activeRiderName: null,
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      };
      mockHttp.get.mockReturnValue(of(mockFleet));

      let result: any[] | undefined;
      service.getAvailableMotorcycles().subscribe((bikes) => {
        result = bikes;
      });

      expect(mockHttp.get).toHaveBeenCalledWith(
        '/api/v1/fleet/motorcycles',
        expect.objectContaining({
          params: expect.anything(),
        })
      );
      expect(result?.length).toBe(1);
      expect(result?.[0].plate).toBe('MOT2B34');
    });
  });
});
