import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { T06WorkbenchComponent } from './t06-workbench.component';
import {
  MechanicInspectionDetailsResponse,
  ResolveInspectionResponse,
} from '../../models/mechanic.models';

describe('T06WorkbenchComponent (Screen T06 - 55%/45% Decision Desk)', () => {
  let comp: T06WorkbenchComponent;
  let mockMechanicService: any;
  let mockRoute: any;
  let mockRouter: any;

  const mockDetails: MechanicInspectionDetailsResponse = {
    id: 'ins-uuid-1',
    status: 'HUMAN_TRIAGE',
    plate: 'MOT1A23',
    motorcycleModel: 'MOTTU_SPORT_110I',
    riderName: 'Marcos Souza',
    riderPhone: '11987654321',
    hoursSincePickup: 18,
    kmDrivenSincePickup: 35,
    photoDownloadUrl: 'https://cdn.mottu.local/photos/damage.webp',
    fraudSuspected: false,
    fraudReason: null,
    reportedContext: 'RECEIVED_LIKE_THIS',
    safetyRisk: 'SAFE_TO_RIDE',
    reportedOdometerKm: 1035,
    riderNotes: 'Ralado no manete já existia ao sair do pátio',
    aiSuggestedVerdict: 'MAU_USO',
    aiConfidence: 0.88,
    aiTechnicalSummary: 'Queda lateral compatível com impacto recente.',
    contestedOnce: true,
    contestationReason: 'Esse ralado já constava quando saí do portão da filial.',
    finalVerdict: null,
    laborFeeBrl: null,
    totalEstimatedBrl: null,
    customerFeedbackText: null,
    items: [
      {
        catalogPartId: 'part-1',
        partName: 'Manete de Freio Direito',
        unitPriceBrl: 25.0,
        addedBy: 'AI_AGENT',
      },
      {
        catalogPartId: 'part-2',
        partName: 'Espelho Retrovisor Direito',
        unitPriceBrl: 42.5,
        addedBy: 'AI_AGENT',
      },
    ],
    createdAt: '2026-11-07T07:00:00Z',
    resolvedAt: null,
  };

  beforeEach(() => {
    mockMechanicService = {
      getInspectionDetails: vi.fn().mockReturnValue(of(mockDetails)),
      resolveInspection: vi.fn(),
    };
    mockRoute = {
      snapshot: {
        paramMap: {
          get: vi.fn().mockReturnValue('ins-uuid-1'),
        },
      },
    };
    mockRouter = {
      navigate: vi.fn(),
    };

    comp = new T06WorkbenchComponent(mockMechanicService, mockRoute, mockRouter);
  });

  it('TC-T06-1: Loads inspection details on init and preselects AI parts', () => {
    comp.ngOnInit();
    expect(mockMechanicService.getInspectionDetails).toHaveBeenCalledWith('ins-uuid-1');
    expect(comp.details()?.plate).toBe('MOT1A23');
    expect(comp.selectedPartIds().size).toBe(2);
    expect(comp.selectedVerdict()).toBe('MAU_USO');
  });

  it('TC-T06-2: Reactive quotation: PRE_EXISTENTE yields R$ 0.00 total', () => {
    comp.ngOnInit();
    comp.setVerdict('PRE_EXISTENTE');

    expect(comp.partsCost()).toBe(0.0);
    expect(comp.laborCost()).toBe(0.0);
    expect(comp.totalEstimated()).toBe(0.0);
  });

  it('TC-T06-3: Reactive quotation: DESGASTE_NATURAL yields R$ 0.00 total', () => {
    comp.ngOnInit();
    comp.setVerdict('DESGASTE_NATURAL');

    expect(comp.partsCost()).toBe(0.0);
    expect(comp.laborCost()).toBe(0.0);
    expect(comp.totalEstimated()).toBe(0.0);
  });

  it('TC-T06-4: Reactive quotation: MAU_USO calculates selected parts sum + R$ 35.00 labor', () => {
    comp.ngOnInit();
    comp.setVerdict('MAU_USO');

    // Both parts selected: 25.00 + 42.50 = 67.50 parts + 35.00 labor = 102.50
    expect(comp.partsCost()).toBe(67.5);
    expect(comp.laborCost()).toBe(35.0);
    expect(comp.totalEstimated()).toBe(102.5);
  });

  it('TC-T06-5: Toggling part on/off dynamically recalculates quotation', () => {
    comp.ngOnInit();
    comp.setVerdict('MAU_USO');

    // Deselect part-2
    comp.togglePart('part-2');
    expect(comp.selectedPartIds().has('part-2')).toBe(false);
    expect(comp.partsCost()).toBe(25.0);
    expect(comp.totalEstimated()).toBe(60.0);

    // Re-select part-2
    comp.togglePart('part-2');
    expect(comp.selectedPartIds().has('part-2')).toBe(true);
    expect(comp.partsCost()).toBe(67.5);
    expect(comp.totalEstimated()).toBe(102.5);
  });

  it('TC-T06-6: resolveInspection submits payload and sets resolution feedback', () => {
    comp.ngOnInit();
    comp.setVerdict('MAU_USO');
    comp.mechanicNotes.set('Ralado profundo com entortamento');

    const mockResponse: ResolveInspectionResponse = {
      inspectionId: 'ins-uuid-1',
      status: 'HUMAN_RESOLVED',
      finalVerdict: 'MAU_USO',
      totalEstimatedBrl: 102.5,
      customerFeedbackText:
        'Olá Marcos, após análise detalhada, identificamos dano recente no manete e retrovisor.',
    };
    mockMechanicService.resolveInspection.mockReturnValue(of(mockResponse));

    comp.resolveInspection();

    expect(mockMechanicService.resolveInspection).toHaveBeenCalledWith('ins-uuid-1', {
      finalVerdict: 'MAU_USO',
      catalogPartIds: ['part-1', 'part-2'],
      mechanicNotes: 'Ralado profundo com entortamento',
    });
    expect(comp.resolutionResult()).toEqual(mockResponse);
    expect(comp.isSubmitting()).toBe(false);
  });

  it('TC-T06-7: Cannot submit if inspection status is not HUMAN_TRIAGE', () => {
    mockDetails.status = 'HUMAN_RESOLVED';
    comp.ngOnInit();

    expect(comp.canSubmit()).toBe(false);
    comp.resolveInspection();
    expect(mockMechanicService.resolveInspection).not.toHaveBeenCalled();
  });
});
