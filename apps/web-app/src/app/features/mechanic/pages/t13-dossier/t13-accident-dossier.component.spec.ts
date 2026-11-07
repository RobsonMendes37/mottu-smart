import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { T13AccidentDossierComponent } from './t13-accident-dossier.component';
import { MechanicAccidentDetailsResponse } from '../../models/accident-dossier.models';

describe('T13AccidentDossierComponent (Screen T13 - Dossiê de Acidente na Oficina)', () => {
  let comp: T13AccidentDossierComponent;
  let accidentMechanicServiceMock: any;
  let routeMock: any;
  let routerMock: any;

  const mockDetails: MechanicAccidentDetailsResponse = {
    id: 'rep-uuid-1',
    status: 'ESTIMATED_PENDING_WORKSHOP',
    plate: 'BAT1A23',
    model: 'MOTTU_SPORT_110I',
    riderName: 'Carlos Silva',
    riderPhone: '11988887777',
    videoDownloadUrl: 'http://minio/video.mp4',
    photoDownloadUrls: ['http://minio/photo1.webp'],
    drivability: 'NEEDS_TOWING',
    hasInjuries: false,
    involvesThirdParty: false,
    reportedOdometerKm: 1250,
    riderNotes: 'Queda lateral',
    riderToMechanicNotes: 'Verificar freio',
    aiConfidence: 0.94,
    structuralRiskAlert: false,
    aiTechnicalSummary: 'Queda lateral direita',
    partsSubtotalBrl: 147.5,
    laborFeeBrl: 90.0,
    towingFeeBrl: 80.0,
    totalEstimatedBrl: 317.5,
    items: [
      {
        catalogPartId: 'part-1',
        partName: 'Manete de Freio Direito',
        unitPriceBrl: 25.0,
        quantity: 1,
        videoTimestampSeconds: 6,
      },
      {
        catalogPartId: 'part-2',
        partName: 'Retrovisor Direito',
        unitPriceBrl: 42.5,
        quantity: 1,
        videoTimestampSeconds: 14,
      },
      {
        catalogPartId: 'part-3',
        partName: 'Pedal de Apoio Dianteiro Direito',
        unitPriceBrl: 80.0,
        quantity: 1,
        videoTimestampSeconds: 21,
      },
    ],
    createdAt: '2026-11-07T12:00:00Z',
  };

  beforeEach(() => {
    accidentMechanicServiceMock = {
      getAccidentDetails: vi.fn().mockReturnValue(of(mockDetails)),
      confirmAccident: vi.fn().mockReturnValue(
        of({ accidentReportId: 'rep-uuid-1', status: 'WORKSHOP_CONFIRMED', totalEstimatedBrl: 317.5, confirmedAt: '2026-11-07T14:00:00Z' })
      ),
    };

    routeMock = {
      snapshot: {
        paramMap: {
          get: vi.fn().mockReturnValue('rep-uuid-1'),
        },
      },
    };

    routerMock = {
      navigate: vi.fn(),
    };

    comp = new T13AccidentDossierComponent(accidentMechanicServiceMock as any, routeMock as any, routerMock as any);
  });

  it('TC-T13-1: Loads dossier and sets all parts as confirmed by default', () => {
    comp.ngOnInit();
    expect(accidentMechanicServiceMock.getAccidentDetails).toHaveBeenCalledWith('rep-uuid-1');
    expect(comp.details()?.plate).toBe('BAT1A23');
    expect(comp.confirmedPartIds().size).toBe(3);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T13-2: Jumps to timestamp on HTML5 video player', () => {
    comp.videoPlayer = {
      nativeElement: {
        currentTime: 0,
        play: vi.fn().mockResolvedValue(undefined),
      } as any,
    };

    comp.jumpToSecond(6);
    expect(comp.videoPlayer.nativeElement.currentTime).toBe(6);
    expect(comp.videoPlayer.nativeElement.play).toHaveBeenCalled();
  });

  it('TC-T13-3: Recalculates subtotal and tiered labor fee when parts are toggled', () => {
    comp.ngOnInit();

    // Initially 3 parts: subtotal 147.50, labor 90.00, towing 80.00 = 317.50
    expect(comp.calculateConfirmedSubtotal()).toBe(147.5);
    expect(comp.calculateConfirmedLabor()).toBe(90.0);
    expect(comp.calculateConfirmedTotal()).toBe(317.5);

    // Uncheck part-3 (80.00) => 2 parts remain
    // 2 parts: subtotal 67.50, labor (1-2 parts) 35.00, towing 80.00 = 182.50
    comp.togglePartConfirmation('part-3');
    expect(comp.calculateConfirmedSubtotal()).toBe(67.5);
    expect(comp.calculateConfirmedLabor()).toBe(35.0);
    expect(comp.calculateConfirmedTotal()).toBe(182.5);
  });

  it('TC-T13-4: Submits confirmation and transitions status to WORKSHOP_CONFIRMED', () => {
    comp.ngOnInit();
    comp.mechanicNotes.set('Reparo liberado');
    comp.onConfirmAccident();

    expect(accidentMechanicServiceMock.confirmAccident).toHaveBeenCalledWith('rep-uuid-1', {
      confirmedPartIds: expect.arrayContaining(['part-1', 'part-2', 'part-3']),
      mechanicNotes: 'Reparo liberado',
    });
    expect(comp.details()?.status).toBe('WORKSHOP_CONFIRMED');
  });

  it('TC-T13-5: Navigates back to queue on back click', () => {
    comp.onBackToQueue();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/mechanic/queue']);
  });
});

