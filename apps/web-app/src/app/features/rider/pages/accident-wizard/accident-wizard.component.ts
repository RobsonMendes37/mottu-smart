import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AccidentService } from '../../services/accident.service';
import {
  CreateAccidentReportCommand,
  VehicleDrivability,
} from '../../models/accident.models';
import { generateUuid } from '../../../../core/utils/uuid.util';

@Component({
  selector: 'app-accident-wizard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accident-wizard.component.html',
})
export class AccidentWizardComponent {
  private accidentService: AccidentService;
  private router: Router | null;

  videoFile = signal<File | null>(null);
  videoDuration = signal<number>(0);
  photoFiles = signal<(File | null)[]>([null, null, null]);
  photoPreviews = signal<(string | null)[]>([null, null, null]);

  hasInjuries = signal<boolean>(false);
  involvesThirdParty = signal<boolean>(false);
  policeReportNumber = signal<string>('');
  drivability = signal<VehicleDrivability>('NEEDS_TOWING');
  reportedOdometerKm = signal<number | null>(null);
  riderNotes = signal<string>('');
  riderToMechanicNotes = signal<string>('');

  isSubmitting = signal<boolean>(false);
  uploadStatusMessage = signal<string>('Enviando...');
  errorMessage = signal<string | null>(null);

  constructor(accidentService?: AccidentService, router?: Router) {
    try {
      this.accidentService = accidentService ?? inject(AccidentService);
    } catch {
      this.accidentService = accidentService as AccidentService;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
  }

  onBack(): void {
    this.router?.navigate(['/rider/garage']);
  }

  async onVideoSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.errorMessage.set(null);

    const check = await this.accidentService.validateVideoDuration(file);
    if (!check.valid) {
      this.errorMessage.set(check.error || 'Vídeo inválido');
      this.videoFile.set(null);
      this.videoDuration.set(0);
      return;
    }

    this.videoFile.set(file);
    this.videoDuration.set(check.durationSeconds);
  }

  onPhotoSelected(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    const updatedFiles = [...this.photoFiles()];
    updatedFiles[index] = file;
    this.photoFiles.set(updatedFiles);

    const reader = new FileReader();
    reader.onload = (e) => {
      const updatedPreviews = [...this.photoPreviews()];
      updatedPreviews[index] = e.target?.result as string;
      this.photoPreviews.set(updatedPreviews);
    };
    reader.readAsDataURL(file);
  }

  toggleInjuries(): void {
    this.hasInjuries.update((v) => !v);
  }

  toggleThirdParty(): void {
    this.involvesThirdParty.update((v) => !v);
  }

  setDrivability(mode: VehicleDrivability): void {
    this.drivability.set(mode);
  }

  onPoliceReportInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.policeReportNumber.set(val);
  }

  onOdometerInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    const num = parseInt(val, 10);
    this.reportedOdometerKm.set(isNaN(num) ? null : num);
  }

  onRiderNotesInput(event: Event): void {
    const val = (event.target as HTMLTextAreaElement).value;
    this.riderNotes.set(val);
  }

  onRiderToMechanicNotesInput(event: Event): void {
    const val = (event.target as HTMLTextAreaElement).value;
    this.riderToMechanicNotes.set(val);
  }

  async onSubmit(): Promise<void> {
    const video = this.videoFile();
    const odo = this.reportedOdometerKm();

    if (!video) {
      this.errorMessage.set('Selecione um vídeo gravado da moto.');
      return;
    }
    if (!odo || odo <= 0) {
      this.errorMessage.set('Informe a quilometragem atual do odômetro.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.uploadStatusMessage.set('Obtendo permissão de upload...');

    try {
      // 1. Get presigned video upload URL
      const contentType = video.type || 'video/mp4';
      const duration = this.videoDuration() || 20;

      const videoPresigned = await firstValueFrom(
        this.accidentService.getVideoUploadUrl({
          contentType,
          fileSizeBytes: video.size,
          durationSeconds: duration,
        })
      );

      // 2. Upload video binary to MinIO
      this.uploadStatusMessage.set('Enviando vídeo do acidente...');
      await this.accidentService.uploadToStorage(videoPresigned.uploadUrl, video, contentType);

      // 3. Upload photos if selected
      const uploadedPhotoKeys: string[] = [];
      const selectedPhotos = this.photoFiles().filter((p): p is File => p !== null);

      if (selectedPhotos.length > 0) {
        this.uploadStatusMessage.set('Enviando fotos de detalhe...');
        for (const photo of selectedPhotos) {
          const photoType = photo.type || 'image/jpeg';
          const photoPresigned = await firstValueFrom(
            this.accidentService.getPhotoUploadUrl({
              contentType: photoType,
              fileSizeBytes: photo.size,
            })
          );
          await this.accidentService.uploadToStorage(photoPresigned.uploadUrl, photo, photoType);
          uploadedPhotoKeys.push(photoPresigned.photoObjectKey);
        }
      }

      // 4. Create accident report
      this.uploadStatusMessage.set('Registrando chamado e acionando IA...');
      const idempotencyKey = generateUuid();

      const command: CreateAccidentReportCommand = {
        videoObjectKey: videoPresigned.videoObjectKey,
        videoDurationSeconds: duration,
        videoSizeBytes: video.size,
        photoObjectKeys: uploadedPhotoKeys,
        hasInjuries: this.hasInjuries(),
        involvesThirdParty: this.involvesThirdParty(),
        policeReportNumber: this.involvesThirdParty() ? this.policeReportNumber() : null,
        drivability: this.drivability(),
        reportedOdometerKm: odo,
        riderNotes: this.riderNotes(),
        riderToMechanicNotes: this.riderToMechanicNotes(),
      };

      const result = await firstValueFrom(
        this.accidentService.createAccidentReport(command, idempotencyKey)
      );

      // 5. Navigate to Quote & Live Status screen T12
      this.router?.navigate(['/rider/accidents', result.accidentReportId]);
    } catch (err: any) {
      this.isSubmitting.set(false);
      this.errorMessage.set(
        err?.error?.message || err?.message || 'Falha ao enviar relato de acidente. Tente novamente.'
      );
    }
  }
}

