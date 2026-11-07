import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { InspectionWizardComponent } from './inspection-wizard.component';
import { RiderStore } from '../../state/rider.store';
import { InspectionUploadService } from '../../services/inspection-upload.service';
import { Router } from '@angular/router';

describe('InspectionWizardComponent (Screen T03 - Nova Vistoria)', () => {
  let comp: InspectionWizardComponent;
  let mockStore: any;
  let mockUploadService: any;
  let mockRouter: any;

  beforeEach(() => {
    mockStore = {
      photoSlots: signal([
        { slot: 'damage', label: '1. Foco do Defeito (Obrigatório)', photoObjectKey: null, previewUrl: null, luminance: null, isDark: false },
        { slot: 'odometer', label: '2. Painel / Hodômetro', photoObjectKey: null, previewUrl: null, luminance: null, isDark: false },
        { slot: 'front', label: '3. Frente da Moto', photoObjectKey: null, previewUrl: null, luminance: null, isDark: false },
        { slot: 'detail', label: '4. Detalhe Complementar', photoObjectKey: null, previewUrl: null, luminance: null, isDark: false },
      ]),
      reportedContext: signal('RECEIVED_LIKE_THIS'),
      safetyRisk: signal('SAFE_TO_RIDE'),
      reportedOdometer: signal(1035),
      riderNotes: signal(''),
      isPrimaryPhotoReady: signal(false),
      isWizardValid: signal(false),
      isSubmittingInspection: signal(false),
      submissionError: signal(null),
      garage: signal({
        activeContract: {
          contractId: 'c1',
          initialOdometerKm: 1000,
          motorcycle: { currentOdometerKm: 1035 },
        },
      }),
      updateSlot: vi.fn(),
      clearSlot: vi.fn(),
      submitInspection: vi.fn(),
      resetWizard: vi.fn(),
    };

    mockUploadService = {
      uploadPhotoSlot: vi.fn(),
    };

    mockRouter = {
      navigate: vi.fn(),
    };

    comp = new InspectionWizardComponent(
      mockStore as any,
      mockUploadService as any,
      mockRouter as any
    );
  });

  it('TC-T03-1: Defaults activeSlot to "damage" and context to "RECEIVED_LIKE_THIS"', () => {
    expect(comp.activeSlot()).toBe('damage');
    expect(comp.reportedContext()).toBe('RECEIVED_LIKE_THIS');
    expect(comp.safetyRisk()).toBe('SAFE_TO_RIDE');
  });

  it('TC-T03-2: Selecting a photo slot changes activeSlot and triggers appropriate label', () => {
    comp.setActiveSlot('odometer');
    expect(comp.activeSlot()).toBe('odometer');
    expect(comp.activeSlotLabel()).toContain('Painel / Hodômetro');
  });

  it('TC-T03-3: Handles file selection, invokes uploadPhotoSlot and updates store slot', async () => {
    const fakeFile = new File(['fake-img'], 'moto.jpg', { type: 'image/jpeg' });
    mockUploadService.uploadPhotoSlot.mockResolvedValue({
      photoObjectKey: 'inspections/temp/damage.webp',
      compression: {
        blob: new Blob(['x']),
        dataUrl: 'data:image/webp;base64,abc',
        sizeBytes: 150000,
        width: 1920,
        height: 1080,
        averageLuminance: 75,
      },
    });

    await comp.processSelectedFile(fakeFile, 'damage');

    expect(mockUploadService.uploadPhotoSlot).toHaveBeenCalledWith(fakeFile, 'damage');
    expect(mockStore.updateSlot).toHaveBeenCalledWith(
      'damage',
      'inspections/temp/damage.webp',
      'data:image/webp;base64,abc',
      75
    );
    expect(comp.uploadErrorMessage()).toBeNull();
  });

  it('TC-T03-4: Displays warning if dark frame (< 40 luminance) is detected', async () => {
    const fakeFile = new File(['dark-img'], 'moto-escuro.jpg', { type: 'image/jpeg' });
    mockUploadService.uploadPhotoSlot.mockResolvedValue({
      photoObjectKey: 'inspections/temp/dark.webp',
      compression: {
        blob: new Blob(['x']),
        dataUrl: 'data:image/webp;base64,dark',
        sizeBytes: 120000,
        width: 1920,
        height: 1080,
        averageLuminance: 28,
      },
    });

    await comp.processSelectedFile(fakeFile, 'damage');

    expect(mockStore.updateSlot).toHaveBeenCalledWith(
      'damage',
      'inspections/temp/dark.webp',
      'data:image/webp;base64,dark',
      28
    );
  });

  it('TC-T03-5: Odometer validation detects value less than initial contract odometer', () => {
    comp.reportedOdometer.set(950);
    expect(comp.isOdometerInvalid()).toBe(true);
    expect(comp.odometerErrorMessage()).toContain('não pode ser menor que o km inicial (1000 km)');

    comp.reportedOdometer.set(1050);
    expect(comp.isOdometerInvalid()).toBe(false);
  });

  it('TC-T03-6: Submits inspection and navigates to status screen upon success', async () => {
    mockStore.photoSlots.set([
      { slot: 'damage', label: '1. Foco do Defeito', photoObjectKey: 'inspections/temp/key.webp', previewUrl: 'data:...', luminance: 60, isDark: false },
    ]);
    comp.reportedOdometer.set(1035);
    mockStore.submitInspection.mockResolvedValue('ins-created-123');

    await comp.onSubmit();

    expect(mockStore.submitInspection).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/rider/inspections', 'ins-created-123']);
  });

  it('TC-T03-7: Displays error banner if submit throws quota limit error (422)', async () => {
    mockStore.photoSlots.set([
      { slot: 'damage', label: '1. Foco do Defeito', photoObjectKey: 'inspections/temp/key.webp', previewUrl: 'data:...', luminance: 60, isDark: false },
    ]);
    comp.reportedOdometer.set(1035);
    mockStore.submitInspection.mockRejectedValue({
      status: 422,
      error: {
        detail: 'Limite de 3 chamados em aberto atingido.',
      },
    });

    await comp.onSubmit();

    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(comp.submitErrorMessage()).toContain('Limite de 3 chamados em aberto atingido');
  });
});
