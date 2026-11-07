import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { InspectionStatusComponent } from './inspection-status.component';
import { RiderStore } from '../../state/rider.store';
import { AuthService } from '../../../../core/services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';

describe('InspectionStatusComponent (Screen T04 - SSE Live Status & Contest/Booking)', () => {
  let comp: InspectionStatusComponent;
  let mockStore: any;
  let mockAuthService: any;
  let mockRouter: any;
  let mockActivatedRoute: any;

  beforeEach(() => {
    mockStore = {
      currentInspection: signal({
        id: 'ins-123',
        status: 'AUTO_RESOLVED',
        photoDownloadUrl: 'http://minio/inspections/photo.webp',
        reportedContext: 'ACCIDENT_OR_DROP',
        safetyRisk: 'SAFE_TO_RIDE',
        reportedOdometerKm: 1035,
        riderNotes: 'Quebra manete',
        finalVerdict: 'MAU_USO',
        aiConfidence: 0.94,
        laborFeeBrl: 35.0,
        totalEstimatedBrl: 60.0,
        customerFeedbackText: 'Identificamos quebra por queda no manete direito.',
        canContest: true,
        contestedOnce: false,
        items: [
          { catalogPartId: 'part-1', partName: 'Manete de Freio', unitPriceBrl: 25.0, addedBy: 'AI' },
        ],
        appointment: null,
        createdAt: '2026-11-07T10:00:00Z',
        resolvedAt: '2026-11-07T10:01:00Z',
      }),
      currentLiveStatus: signal('AUTO_RESOLVED'),
      liveStepLabel: signal('✓ Concluído'),
      isStreamingSse: signal(false),
      yards: signal([
        { id: 'yard-1', name: 'Galpão Butantã', city: 'São Paulo', address: 'Av Vital Brasil, 500', active: true },
        { id: 'yard-2', name: 'Galpão Pinheiros', city: 'São Paulo', address: 'Rua Teodoro', active: true },
      ]),
      isBookingAppointment: signal(false),
      isContesting: signal(false),
      loadInspectionDetails: vi.fn().mockResolvedValue(undefined),
      startLiveStream: vi.fn(),
      stopLiveStream: vi.fn(),
      loadYards: vi.fn().mockResolvedValue(undefined),
      contestInspection: vi.fn().mockResolvedValue(undefined),
      bookAppointment: vi.fn().mockResolvedValue(undefined),
    };

    mockAuthService = {
      accessToken: signal('mock-jwt-token'),
    };

    mockRouter = {
      navigate: vi.fn(),
    };

    mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: (key: string) => (key === 'id' ? 'ins-123' : null),
        },
      },
    };

    comp = new InspectionStatusComponent(
      mockStore as any,
      mockAuthService as any,
      mockRouter as any,
      mockActivatedRoute as any
    );
    comp.ngOnInit();
  });

  it('TC-T04-1: Initializes by loading inspection details and starting live stream', () => {
    comp.ngOnInit();
    expect(mockStore.loadInspectionDetails).toHaveBeenCalledWith('ins-123');
    expect(mockStore.startLiveStream).toHaveBeenCalledWith('ins-123');
    expect(mockStore.loadYards).toHaveBeenCalled();
  });

  it('TC-T04-2: Unsubscribes/stops live stream on destroy', () => {
    comp.ngOnDestroy();
    expect(mockStore.stopLiveStream).toHaveBeenCalled();
  });

  it('TC-T04-3: Displays budget total and item breakdown for MAU_USO verdict', () => {
    expect(comp.totalPriceDisplay()).toBe('R$ 60,00');
    expect(comp.isMauUso()).toBe(true);
    expect(comp.isZeroCost()).toBe(false);
  });

  it('TC-T04-4: Displays R$ 0,00 and zero cost tag for PRE_EXISTENTE verdict', () => {
    mockStore.currentInspection.set({
      ...mockStore.currentInspection(),
      finalVerdict: 'PRE_EXISTENTE',
      totalEstimatedBrl: 0,
    });

    expect(comp.totalPriceDisplay()).toBe('R$ 0,00');
    expect(comp.isZeroCost()).toBe(true);
    expect(comp.isMauUso()).toBe(false);
  });

  it('TC-T04-5: Contestation enforces minimum 10 characters before submitting', async () => {
    comp.openContestModal();
    expect(comp.isContestModalOpen()).toBe(true);

    // Reason with < 10 characters
    comp.contestReason.set('Não fui');
    expect(comp.isContestReasonValid()).toBe(false);

    await comp.onSubmitContestation();
    expect(mockStore.contestInspection).not.toHaveBeenCalled();

    // Reason with >= 10 characters
    comp.contestReason.set('A moto já veio com esse dano quando retirei no galpão.');
    expect(comp.isContestReasonValid()).toBe(true);

    await comp.onSubmitContestation();
    expect(mockStore.contestInspection).toHaveBeenCalledWith(
      'ins-123',
      'A moto já veio com esse dano quando retirei no galpão.'
    );
    expect(comp.isContestModalOpen()).toBe(false);
  });

  it('TC-T04-6: Yard appointment modal enforces date validation and calls store.bookAppointment', async () => {
    comp.openAppointmentModal();
    expect(comp.isAppointmentModalOpen()).toBe(true);

    comp.selectedYardId.set('yard-1');
    comp.selectedDate.set('2026-11-15');
    comp.selectedShift.set('AFTERNOON');

    await comp.onSubmitAppointment();

    expect(mockStore.bookAppointment).toHaveBeenCalledWith(
      'ins-123',
      'yard-1',
      '2026-11-15',
      'AFTERNOON'
    );
    expect(comp.isAppointmentModalOpen()).toBe(false);
  });
});
