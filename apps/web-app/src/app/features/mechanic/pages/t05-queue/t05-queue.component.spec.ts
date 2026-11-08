import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { T05QueueComponent } from './t05-queue.component';
import { MechanicQueueItemDto, PageResponse } from '../../models/mechanic.models';

describe('T05QueueComponent (Screen T05 - Intelligent Queue)', () => {
  let comp: T05QueueComponent;
  let mockMechanicService: any;
  let mockRouter: any;

  const mockItems: MechanicQueueItemDto[] = [
    {
      inspectionId: 'ins-1',
      plate: 'MOT8K11',
      motorcycleModel: 'MOTTU_SPORT_110I',
      riderName: 'Carlos Silva',
      priorityWeight: 1,
      priorityReason: 'CRITICAL_SAFETY',
      safetyRisk: 'CRITICAL_STOPPED',
      fraudSuspected: false,
      aiConfidence: 0.95,
      aiSuggestedVerdict: 'MAU_USO',
      createdAt: '2026-11-07T07:00:00Z',
    },
    {
      inspectionId: 'ins-2',
      plate: 'MOT1A23',
      motorcycleModel: 'MOTTU_SPORT_110I',
      riderName: 'Marcos Souza',
      priorityWeight: 2,
      priorityReason: 'RIDER_CONTESTATION',
      safetyRisk: 'SAFE_TO_RIDE',
      fraudSuspected: false,
      aiConfidence: 0.88,
      aiSuggestedVerdict: 'MAU_USO',
      createdAt: '2026-11-07T07:05:00Z',
    },
    {
      inspectionId: 'ins-3',
      plate: 'POP3C45',
      motorcycleModel: 'HONDA_POP_110I',
      riderName: 'Lucas Lima',
      priorityWeight: 3,
      priorityReason: 'LOW_AI_CONFIDENCE',
      safetyRisk: 'SAFE_TO_RIDE',
      fraudSuspected: false,
      aiConfidence: 0.64,
      aiSuggestedVerdict: 'DESGASTE_NATURAL',
      createdAt: '2026-11-07T07:10:00Z',
    },
    {
      inspectionId: 'ins-4',
      plate: 'TVS4D56',
      motorcycleModel: 'TVS_APACHE_160',
      riderName: 'Rafael Costa',
      priorityWeight: 4,
      priorityReason: 'FRAUD_SUSPICION',
      safetyRisk: 'SAFE_TO_RIDE',
      fraudSuspected: true,
      aiConfidence: 0.9,
      aiSuggestedVerdict: 'PRE_EXISTENTE',
      createdAt: '2026-11-07T07:15:00Z',
    },
  ];

  const mockPageResponse: PageResponse<MechanicQueueItemDto> = {
    items: mockItems,
    page: 0,
    size: 20,
    totalElements: 4,
    totalPages: 1,
  };

  beforeEach(() => {
    mockMechanicService = {
      getQueue: vi.fn().mockReturnValue(of(mockPageResponse)),
    };
    mockRouter = {
      navigate: vi.fn(),
    };

    comp = new T05QueueComponent(mockMechanicService, mockRouter);
  });

  it('TC-T05-1: ngOnInit loads queue items and sets totalElements', () => {
    comp.ngOnInit();
    expect(mockMechanicService.getQueue).toHaveBeenCalledWith(undefined, '', 0, 20);
    expect(comp.queueItems().length).toBe(4);
    expect(comp.totalElements()).toBe(4);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T05-2: setReason filters queue by priority reason and reloads', () => {
    comp.setReason('CRITICAL_SAFETY');
    expect(comp.selectedReason()).toBe('CRITICAL_SAFETY');
    expect(mockMechanicService.getQueue).toHaveBeenCalledWith('CRITICAL_SAFETY', '', 0, 20);
  });

  it('TC-T05-3: onSearchPlate filters queue by plate', () => {
    comp.onSearchPlate('MOT1A23');
    expect(comp.searchPlate()).toBe('MOT1A23');
    expect(mockMechanicService.getQueue).toHaveBeenCalledWith(undefined, 'MOT1A23', 0, 20);
  });

  it('TC-T05-4: Priority color and styling adheres to DESIGN.md v2.0 & Purple Ban (P4 uses slate/sky, zero purple)', () => {
    const badgeP1 = comp.getPriorityBadge('CRITICAL_SAFETY', 1);
    expect(badgeP1.label).toContain('P1');
    expect(badgeP1.colorClass).toContain('text-[#B91C1C]');

    const badgeP2 = comp.getPriorityBadge('RIDER_CONTESTATION', 2);
    expect(badgeP2.label).toContain('P2');
    expect(badgeP2.colorClass).toContain('text-[#92400E]');

    const badgeP3 = comp.getPriorityBadge('LOW_AI_CONFIDENCE', 3);
    expect(badgeP3.label).toContain('P3');
    expect(badgeP3.colorClass).toContain('text-[#92400E]');

    const badgeP4 = comp.getPriorityBadge('FRAUD_SUSPICION', 4);
    expect(badgeP4.label).toContain('P4');
    expect(badgeP4.colorClass).toContain('text-[#334155]');
    // Strict Purple Ban verification:
    expect(badgeP4.colorClass).not.toMatch(/purple|violet|indigo/);
  });

  it('TC-T05-5: openWorkbench navigates to /mechanic/workbench/:id', () => {
    comp.openWorkbench('ins-1');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/mechanic/workbench', 'ins-1']);
  });

  it('TC-T05-6: Error loading queue sets errorMessage', () => {
    mockMechanicService.getQueue.mockReturnValue(throwError(() => new Error('Falha de rede')));
    comp.loadQueue();
    expect(comp.errorMessage()).toBe('Não foi possível carregar a fila da oficina.');
    expect(comp.isLoading()).toBe(false);
  });
});
