import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { T09CatalogComponent } from './t09-catalog.component';
import {
  CatalogPartResponseDto,
  PageResponse,
  SyncCatalogResponseDto,
  ManagerSettingsDto,
} from '../../models/manager.models';

describe('T09CatalogComponent (Screen T09 - Official Parts Catalog & Sync)', () => {
  let comp: T09CatalogComponent;
  let mockManagerService: any;

  const mockParts: CatalogPartResponseDto[] = [
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
    {
      id: 'part-2',
      vtexSku: 'SKU-1002',
      motorcycleModel: 'MOTTU_SPORT_110I',
      partName: 'Jogo Lona/Pastilha Freio',
      category: 'FREIOS',
      unitPriceBrl: 33.0,
      isNaturalWear: true,
      priceOverridden: false,
      imageUrl: null,
      updatedAt: '2026-11-01T10:00:00Z',
    },
    {
      id: 'part-3',
      vtexSku: 'SKU-2040',
      motorcycleModel: 'HONDA_POP_110I',
      partName: 'Retrovisor Direito',
      category: 'CARENAGEM',
      unitPriceBrl: 42.5,
      isNaturalWear: false,
      priceOverridden: true,
      imageUrl: null,
      updatedAt: '2026-11-02T11:00:00Z',
    },
  ];

  const mockPage: PageResponse<CatalogPartResponseDto> = {
    items: mockParts,
    page: 0,
    size: 20,
    totalElements: 3,
    totalPages: 1,
  };

  const mockSettings: ManagerSettingsDto = {
    standardLaborFeeBrl: 35.0,
  };

  beforeEach(() => {
    mockManagerService = {
      getParts: vi.fn().mockReturnValue(of(mockPage)),
      getSettings: vi.fn().mockReturnValue(of(mockSettings)),
      updateSettings: vi.fn().mockImplementation((req: ManagerSettingsDto) => of(req)),
      syncMottuStore: vi.fn().mockReturnValue(
        of<SyncCatalogResponseDto>({
          syncedCount: 42,
          usedFallback: false,
          syncedAt: '2026-11-07T10:15:00Z',
          message: 'Sincronizado diretamente do site loja.mottu.com.br',
        })
      ),
      updatePart: vi.fn().mockImplementation((id: string, req: any) => {
        const found = mockParts.find((p) => p.id === id);
        return of<CatalogPartResponseDto>({
          ...found!,
          unitPriceBrl: req.unitPriceBrl ?? found!.unitPriceBrl,
          isNaturalWear: req.isNaturalWear ?? found!.isNaturalWear,
          priceOverridden: true,
          updatedAt: '2026-11-07T10:20:00Z',
        });
      }),
    };

    comp = new T09CatalogComponent(mockManagerService);
  });

  it('TC-T09-1: ngOnInit loads parts and labor fee settings', () => {
    comp.ngOnInit();
    expect(mockManagerService.getParts).toHaveBeenCalled();
    expect(mockManagerService.getSettings).toHaveBeenCalled();
    expect(comp.parts().length).toBe(3);
    expect(comp.standardLaborFee()).toBe(35.0);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T09-2: filterByModel reloads parts with model filter', () => {
    comp.setModelFilter('HONDA_POP_110I');
    expect(comp.selectedModel()).toBe('HONDA_POP_110I');
    expect(mockManagerService.getParts).toHaveBeenCalledWith('HONDA_POP_110I', '', 0, 20);
  });

  it('TC-T09-3: onSearch reloads parts with search text', () => {
    comp.onSearch('manete');
    expect(comp.searchTerm()).toBe('manete');
    expect(mockManagerService.getParts).toHaveBeenCalledWith(undefined, 'manete', 0, 20);
  });

  it('TC-T09-4: syncMottuStore triggers sync and displays feedback banner', () => {
    comp.syncMottuStore();
    expect(mockManagerService.syncMottuStore).toHaveBeenCalled();
    expect(comp.isSyncing()).toBe(false);
    expect(comp.syncResult()).not.toBeNull();
    expect(comp.syncResult()?.syncedCount).toBe(42);
    expect(comp.syncResult()?.usedFallback).toBe(false);
    expect(comp.successMessage()).toContain('42 peças sincronizadas');
  });

  it('TC-T09-5: syncMottuStore with usedFallback displays fallback notice', () => {
    mockManagerService.syncMottuStore.mockReturnValue(
      of<SyncCatalogResponseDto>({
        syncedCount: 15,
        usedFallback: true,
        syncedAt: '2026-11-07T10:15:00Z',
        message: 'Modo offline: catálogo restaurado via snapshot',
      })
    );

    comp.syncMottuStore();
    expect(comp.syncResult()?.usedFallback).toBe(true);
    expect(comp.successMessage()).toContain('Modo offline/snapshot utilizado');
  });

  it('TC-T09-6: startEditPart sets editing states and cancelEdit resets them', () => {
    const part = mockParts[0];
    comp.startEditPart(part);

    expect(comp.editingPartId()).toBe(part.id);
    expect(comp.editingPrice()).toBe(25.0);
    expect(comp.editingNaturalWear()).toBe(false);

    comp.cancelEdit();
    expect(comp.editingPartId()).toBeNull();
  });

  it('TC-T09-7: saveEditPart updates price and natural wear and marks priceOverridden', () => {
    comp.ngOnInit();
    const part = mockParts[0];
    comp.startEditPart(part);
    comp.editingPrice.set(29.9);
    comp.editingNaturalWear.set(true);

    comp.saveEditPart();

    expect(mockManagerService.updatePart).toHaveBeenCalledWith('part-1', {
      unitPriceBrl: 29.9,
      isNaturalWear: true,
    });
    expect(comp.editingPartId()).toBeNull();
    const updated = comp.parts().find((p) => p.id === 'part-1');
    expect(updated?.unitPriceBrl).toBe(29.9);
    expect(updated?.isNaturalWear).toBe(true);
    expect(updated?.priceOverridden).toBe(true);
  });

  it('TC-T09-8: saveEditPart rejects negative price', () => {
    const part = mockParts[0];
    comp.startEditPart(part);
    comp.editingPrice.set(-5);

    comp.saveEditPart();

    expect(comp.errorMessage()).toContain('O preço deve ser maior ou igual a zero');
    expect(mockManagerService.updatePart).not.toHaveBeenCalled();
  });

  it('TC-T09-9: updateLaborFee updates standard labor fee setting', () => {
    comp.setLaborFeeInput(40.0);
    comp.saveLaborFee();

    expect(mockManagerService.updateSettings).toHaveBeenCalledWith({
      standardLaborFeeBrl: 40.0,
    });
    expect(comp.standardLaborFee()).toBe(40.0);
    expect(comp.successMessage()).toContain('Taxa padrão de mão de obra atualizada');
  });

  it('TC-T09-10: Exposes editingPart computed for right 400px Slide-over Drawer', () => {
    comp.ngOnInit();
    comp.startEditPart(mockParts[0]);
    expect(comp.editingPart()?.vtexSku).toBe('SKU-1001');

    comp.cancelEdit();
    expect(comp.editingPart()).toBeNull();
  });
});
