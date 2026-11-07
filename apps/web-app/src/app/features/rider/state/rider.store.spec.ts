import { describe, it, expect, beforeEach, vi } from 'vitest';
import { RiderStore } from './rider.store';
import { of, throwError } from 'rxjs';
import { RiderGarageResponse, RiderInspectionDetails } from '../models/rider.models';

describe('RiderStore (Angular 19 Signals State)', () => {
  let store: RiderStore;
  let mockRiderService: any;
  let mockSseService: any;

  beforeEach(() => {
    mockRiderService = {
      getGarage: vi.fn(),
      createInspection: vi.fn(),
      getInspectionDetails: vi.fn(),
      contestInspection: vi.fn(),
      createAppointment: vi.fn(),
      getYards: vi.fn(),
    };
    mockSseService = {
      connect: vi.fn(),
    };
    store = new RiderStore(mockRiderService, mockSseService);
  });

  it('TC-STR-1: Initial store state initializes 4 photo slots and default wizard values', () => {
    expect(store.photoSlots().length).toBe(4);
    expect(store.photoSlots()[0].slot).toBe('damage');
    expect(store.photoSlots()[1].slot).toBe('odometer');
    expect(store.photoSlots()[2].slot).toBe('front');
    expect(store.photoSlots()[3].slot).toBe('detail');
    expect(store.reportedContext()).toBe('RECEIVED_LIKE_THIS');
    expect(store.safetyRisk()).toBe('SAFE_TO_RIDE');
    expect(store.isPrimaryPhotoReady()).toBe(false);
    expect(store.hasReachedQuota()).toBe(false);
  });

  it('TC-STR-2: loadGarage updates garage signal and initializes reportedOdometer from active contract', async () => {
    const mockGarage: RiderGarageResponse = {
      activeContract: {
        contractId: 'contract-1',
        pickedUpAt: '2026-11-06T10:00:00Z',
        initialOdometerKm: 1000,
        motorcycle: {
          id: 'moto-1',
          plate: 'MOT1A23',
          model: 'MOTTU_SPORT_110I',
          status: 'RENTED',
          currentOdometerKm: 1035,
          yardName: 'Butantã',
        },
      },
      openInspectionsCount: 1,
      recentInspections: [],
    };
    mockRiderService.getGarage.mockReturnValue(of(mockGarage));

    await store.loadGarage();

    expect(store.garage()).toEqual(mockGarage);
    expect(store.activeMotorcycle()?.plate).toBe('MOT1A23');
    expect(store.reportedOdometer()).toBe(1035);
    expect(store.hasReachedQuota()).toBe(false);
  });

  it('TC-STR-3: Quota computation flags when openInspectionsCount >= 3', () => {
    store.garage.set({
      activeContract: null,
      openInspectionsCount: 3,
      recentInspections: [],
    });
    expect(store.hasReachedQuota()).toBe(true);
  });

  it('TC-STR-4: updateSlot correctly updates target slot and flags dark frame if luminance < 40', () => {
    store.updateSlot('damage', 'inspections/temp/uuid.webp', 'data:image/webp;base64,...', 32);

    const damageSlot = store.photoSlots().find((s) => s.slot === 'damage');
    expect(damageSlot?.photoObjectKey).toBe('inspections/temp/uuid.webp');
    expect(damageSlot?.luminance).toBe(32);
    expect(damageSlot?.isDark).toBe(true);
    expect(store.isPrimaryPhotoReady()).toBe(true);

    // Update with bright image
    store.updateSlot('damage', 'inspections/temp/uuid-bright.webp', 'data:image/webp;base64,...', 85);
    const updated = store.photoSlots().find((s) => s.slot === 'damage');
    expect(updated?.isDark).toBe(false);
  });

  it('TC-STR-5: isWizardValid requires primary photo and odometer >= initial odometer', () => {
    store.garage.set({
      activeContract: {
        contractId: 'c1',
        pickedUpAt: '2026-11-01',
        initialOdometerKm: 1000,
        motorcycle: {
          id: 'm1',
          plate: 'MOT1A23',
          model: 'MOTTU_SPORT_110I',
          status: 'RENTED',
          currentOdometerKm: 1000,
          yardName: 'Butantã',
        },
      },
      openInspectionsCount: 0,
      recentInspections: [],
    });

    // Case 1: No photo
    store.reportedOdometer.set(1005);
    expect(store.isWizardValid()).toBe(false);

    // Case 2: Photo ready but odometer lower than initial
    store.updateSlot('damage', 'key', 'preview', 70);
    store.reportedOdometer.set(950);
    expect(store.isWizardValid()).toBe(false);

    // Case 3: Photo ready and valid odometer
    store.reportedOdometer.set(1000);
    expect(store.isWizardValid()).toBe(true);
  });

  it('TC-STR-6: submitInspection sends generated Idempotency-Key and returns inspectionId', async () => {
    store.updateSlot('damage', 'inspections/temp/key.webp', 'preview', 70);
    store.reportedOdometer.set(1050);
    store.riderNotes.set('Barulho estranho no manete');

    mockRiderService.createInspection.mockReturnValue(
      of({
        inspectionId: 'ins-new-uuid',
        status: 'QUEUED_AI',
      })
    );

    const inspectionId = await store.submitInspection();

    expect(inspectionId).toBe('ins-new-uuid');
    expect(mockRiderService.createInspection).toHaveBeenCalled();
    const [idempotencyKey, payload] = mockRiderService.createInspection.mock.calls[0];
    expect(typeof idempotencyKey).toBe('string');
    expect(idempotencyKey.length).toBeGreaterThan(10);
    expect(payload.photoObjectKey).toBe('inspections/temp/key.webp');
    expect(payload.reportedOdometerKm).toBe(1050);
    expect(payload.riderNotes).toBe('Barulho estranho no manete');
  });

  it('TC-STR-7: contestInspection submits reason and updates details', async () => {
    const mockDetails: RiderInspectionDetails = {
      id: 'ins-1',
      status: 'HUMAN_TRIAGE',
      photoDownloadUrl: 'url',
      reportedContext: 'RECEIVED_LIKE_THIS',
      safetyRisk: 'SAFE_TO_RIDE',
      reportedOdometerKm: 1035,
      riderNotes: '',
      finalVerdict: 'MAU_USO',
      aiConfidence: 0.95,
      laborFeeBrl: 35,
      totalEstimatedBrl: 60,
      customerFeedbackText: 'Identificamos quebra...',
      canContest: false,
      contestedOnce: true,
      items: [],
      appointment: null,
      createdAt: '2026-11-07T00:00:00Z',
      resolvedAt: null,
    };

    mockRiderService.contestInspection.mockReturnValue(
      of({
        inspectionId: 'ins-1',
        status: 'HUMAN_TRIAGE',
        priorityReason: 'RIDER_CONTESTATION',
      })
    );
    mockRiderService.getInspectionDetails.mockReturnValue(of(mockDetails));

    await store.contestInspection('ins-1', 'A moto já estava com esse risco quando retirei.');

    expect(mockRiderService.contestInspection).toHaveBeenCalledWith('ins-1', {
      reason: 'A moto já estava com esse risco quando retirei.',
    });
    expect(store.currentInspection()).toEqual(mockDetails);
    expect(store.canContestInspection()).toBe(false);
  });

  it('TC-STR-8: bookAppointment creates appointment and updates status', async () => {
    mockRiderService.createAppointment.mockReturnValue(
      of({
        appointmentId: 'apt-1',
        inspectionStatus: 'SCHEDULED',
        yardName: 'Galpão Butantã',
        scheduledDate: '2026-11-10',
        shift: 'MORNING',
      })
    );
    mockRiderService.getInspectionDetails.mockReturnValue(
      of({
        id: 'ins-1',
        status: 'SCHEDULED',
      })
    );

    await store.bookAppointment('ins-1', 'yard-1', '2026-11-10', 'MORNING');

    expect(mockRiderService.createAppointment).toHaveBeenCalledWith('ins-1', {
      yardId: 'yard-1',
      scheduledDate: '2026-11-10',
      shift: 'MORNING',
    });
    expect(store.currentLiveStatus()).toBe('SCHEDULED');
  });
});
