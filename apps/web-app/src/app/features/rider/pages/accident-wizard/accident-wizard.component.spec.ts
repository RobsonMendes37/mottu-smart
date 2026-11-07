import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AccidentWizardComponent } from './accident-wizard.component';
import { AccidentService } from '../../services/accident.service';

describe('AccidentWizardComponent (Screen T11 - Auto-Relato de Acidente)', () => {
  let comp: AccidentWizardComponent;
  let accidentServiceMock: any;
  let routerMock: any;

  beforeEach(() => {
    accidentServiceMock = {
      validateVideoDuration: vi.fn().mockResolvedValue({ durationSeconds: 22, valid: true }),
      getVideoUploadUrl: vi.fn().mockReturnValue(
        of({ uploadUrl: 'http://minio/video.mp4', videoObjectKey: 'accidents/2026/11/video.mp4', expiresInSeconds: 900 })
      ),
      getPhotoUploadUrl: vi.fn().mockReturnValue(
        of({ uploadUrl: 'http://minio/photo.jpg', photoObjectKey: 'accidents/2026/11/photo.jpg', expiresInSeconds: 300 })
      ),
      uploadToStorage: vi.fn().mockResolvedValue(undefined),
      createAccidentReport: vi.fn().mockReturnValue(
        of({ accidentReportId: 'rep-uuid-1', status: 'QUEUED_AI', motorcycleStatus: 'IN_MAINTENANCE' })
      ),
    };

    routerMock = {
      navigate: vi.fn(),
    };

    comp = new AccidentWizardComponent(accidentServiceMock as any, routerMock as any);
  });

  it('TC-T11-1: Navigates back to garage on back button click', () => {
    comp.onBack();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/rider/garage']);
  });

  it('TC-T11-2: Rejects video if duration or size check fails', async () => {
    accidentServiceMock.validateVideoDuration.mockResolvedValue({
      durationSeconds: 35,
      valid: false,
      error: 'O vídeo tem 35 segundos. O limite máximo permitido é de 30 segundos.',
    });

    const fakeFile = new File(['dummy'], 'video.mp4', { type: 'video/mp4' });
    const event = { target: { files: [fakeFile] } } as any;

    await comp.onVideoSelected(event);

    expect(comp.videoFile()).toBeNull();
    expect(comp.errorMessage()).toContain('35 segundos');
  });

  it('TC-T11-3: Toggles injuries and third party switches', () => {
    expect(comp.hasInjuries()).toBe(false);
    comp.toggleInjuries();
    expect(comp.hasInjuries()).toBe(true);

    expect(comp.involvesThirdParty()).toBe(false);
    comp.toggleThirdParty();
    expect(comp.involvesThirdParty()).toBe(true);
  });

  it('TC-T11-4: Submits full report, uploads video and navigates to T12', async () => {
    const fakeFile = new File(['dummy'], 'valid.mp4', { type: 'video/mp4' });
    comp.videoFile.set(fakeFile);
    comp.videoDuration.set(22);
    comp.reportedOdometerKm.set(1250);
    comp.drivability.set('NEEDS_TOWING');

    await comp.onSubmit();

    expect(accidentServiceMock.getVideoUploadUrl).toHaveBeenCalled();
    expect(accidentServiceMock.uploadToStorage).toHaveBeenCalledWith(
      'http://minio/video.mp4',
      fakeFile,
      'video/mp4'
    );
    expect(accidentServiceMock.createAccidentReport).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/rider/accidents', 'rep-uuid-1']);
  });

  it('TC-T11-5: Displays error message when upload fails', async () => {
    accidentServiceMock.uploadToStorage.mockRejectedValue(new Error('Network error uploading video'));

    const fakeFile = new File(['dummy'], 'valid.mp4', { type: 'video/mp4' });
    comp.videoFile.set(fakeFile);
    comp.videoDuration.set(20);
    comp.reportedOdometerKm.set(1200);

    await comp.onSubmit();

    expect(comp.isSubmitting()).toBe(false);
    expect(comp.errorMessage()).toContain('Network error uploading video');
  });
});
