import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { AccidentQuoteComponent } from './accident-quote.component';
import { RiderAccidentReportDetails } from '../../models/accident.models';

describe('AccidentQuoteComponent (Screen T12 - Orçamento Detalhado de Peças Novas)', () => {
  let comp: AccidentQuoteComponent;
  let accidentServiceMock: any;
  let routeMock: any;
  let routerMock: any;

  const mockDetails: RiderAccidentReportDetails = {
    id: 'rep-uuid-1',
    status: 'ESTIMATED_PENDING_WORKSHOP',
    videoDownloadUrl: 'http://minio/video.mp4',
    drivability: 'NEEDS_TOWING',
    hasInjuries: false,
    structuralRiskAlert: false,
    aiConfidence: 0.94,
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
    riderToMechanicNotes: 'Verificar freio',
    createdAt: '2026-11-07T12:00:00Z',
  };

  beforeEach(() => {
    accidentServiceMock = {
      getAccidentDetails: vi.fn().mockReturnValue(of(mockDetails)),
      subscribeAccidentLiveStatus: vi.fn().mockReturnValue(
        of({ accidentReportId: 'rep-uuid-1', status: 'ESTIMATED_PENDING_WORKSHOP', stepLabel: 'Orçamento pronto!' })
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

    comp = new AccidentQuoteComponent(accidentServiceMock as any, routeMock as any, routerMock as any);
  });

  it('TC-T12-1: Loads accident details and turns off loading when estimated', () => {
    comp.ngOnInit();
    expect(accidentServiceMock.getAccidentDetails).toHaveBeenCalledWith('rep-uuid-1');
    expect(comp.details()?.totalEstimatedBrl).toBe(317.5);
    expect(comp.details()?.items.length).toBe(3);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T12-2: Jumps to specific video timestamp when button clicked', () => {
    comp.videoPlayer = {
      nativeElement: {
        currentTime: 0,
        play: vi.fn().mockResolvedValue(undefined),
      } as any,
    };

    comp.jumpToSecond(14);
    expect(comp.videoPlayer.nativeElement.currentTime).toBe(14);
    expect(comp.videoPlayer.nativeElement.play).toHaveBeenCalled();
  });

  it('TC-T12-3: Formats seconds into MM:SS string correctly', () => {
    expect(comp.formatTime(6)).toBe('00:06');
    expect(comp.formatTime(75)).toBe('01:15');
    expect(comp.formatTime(undefined)).toBe('00:00');
  });

  it('TC-T12-4: Navigates back to garage on CTA click', () => {
    comp.onBackToGarage();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/rider/garage']);
  });
});

