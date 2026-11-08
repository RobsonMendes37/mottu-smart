import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RiderStore, PhotoSlotState } from '../../state/rider.store';
import { InspectionUploadService } from '../../services/inspection-upload.service';
import { ReportedOriginContext, SafetyRiskLevel } from '../../models/rider.models';

@Component({
  selector: 'app-inspection-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inspection-wizard.component.html',
})
export class InspectionWizardComponent implements OnInit {
  protected store: RiderStore;
  protected uploadService: InspectionUploadService;
  protected router: Router | null;

  activeSlot = signal<'damage' | 'odometer' | 'front' | 'detail'>('damage');
  isUploading = signal<boolean>(false);
  uploadErrorMessage = signal<string | null>(null);
  submitErrorMessage = signal<string | null>(null);

  // Local signals linked to store
  reportedContext = signal<ReportedOriginContext>('RECEIVED_LIKE_THIS');
  safetyRisk = signal<SafetyRiskLevel>('SAFE_TO_RIDE');
  reportedOdometer = signal<number>(0);
  riderNotes = signal<string>('');

  slots = computed(() => {
    const slots = this.store.photoSlots;
    return typeof slots === 'function' ? slots() : [];
  });

  activeSlotState = computed(() => {
    return this.slots().find((s) => s.slot === this.activeSlot()) || null;
  });

  activeSlotLabel = computed(() => {
    return this.activeSlotState()?.label || 'Foto da Vistoria';
  });

  minOdometer = computed(() => {
    const garage = this.store.garage;
    const g = typeof garage === 'function' ? garage() : null;
    return g?.activeContract?.initialOdometerKm || 0;
  });

  isOdometerInvalid = computed(() => {
    return this.reportedOdometer() < this.minOdometer();
  });

  odometerErrorMessage = computed(() => {
    if (this.isOdometerInvalid()) {
      return `O hodômetro não pode ser menor que o km inicial (${this.minOdometer()} km).`;
    }
    return null;
  });

  isPrimaryPhotoReady = computed(() => {
    const primary = this.slots().find((s) => s.slot === 'damage');
    return !!primary?.photoObjectKey;
  });

  canSubmit = computed(() => {
    return (
      this.isPrimaryPhotoReady() &&
      !this.isOdometerInvalid() &&
      !this.isUploading() &&
      !this.store.isSubmittingInspection()
    );
  });

  constructor(
    store?: RiderStore,
    uploadService?: InspectionUploadService,
    router?: Router
  ) {
    try {
      this.store = store ?? inject(RiderStore);
    } catch {
      this.store = store as RiderStore;
    }
    try {
      this.uploadService = uploadService ?? inject(InspectionUploadService);
    } catch {
      this.uploadService = uploadService as InspectionUploadService;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
  }

  ngOnInit(): void {
    if (this.store.reportedOdometer() > 0) {
      this.reportedOdometer.set(this.store.reportedOdometer());
    } else {
      const garage = this.store.garage();
      if (garage?.activeContract) {
        this.reportedOdometer.set(garage.activeContract.motorcycle.currentOdometerKm);
      }
    }
    this.reportedContext.set(this.store.reportedContext());
    this.safetyRisk.set(this.store.safetyRisk());
  }

  setActiveSlot(slot: 'damage' | 'odometer' | 'front' | 'detail'): void {
    this.activeSlot.set(slot);
    this.uploadErrorMessage.set(null);
  }

  setContext(ctx: ReportedOriginContext): void {
    this.reportedContext.set(ctx);
    this.store.reportedContext.set(ctx);
  }

  setSafetyRisk(risk: SafetyRiskLevel): void {
    this.safetyRisk.set(risk);
    this.store.safetyRisk.set(risk);
  }

  onOdometerChange(value: number): void {
    this.reportedOdometer.set(value);
    this.store.reportedOdometer.set(value);
  }

  onNotesChange(notes: string): void {
    this.riderNotes.set(notes);
    this.store.riderNotes.set(notes);
  }

  triggerFileInput(input: HTMLInputElement): void {
    input.value = '';
    input.click();
  }

  onFileSelected(event: Event, targetSlot?: 'damage' | 'odometer' | 'front' | 'detail'): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const slot = targetSlot || this.activeSlot();
      this.processSelectedFile(file, slot);
    }
    input.value = ''; // Always clear after selection or cancel!
  }

  async processSelectedFile(
    file: File | Blob,
    slot: 'damage' | 'odometer' | 'front' | 'detail'
  ): Promise<void> {
    this.isUploading.set(true);
    this.uploadErrorMessage.set(null);
    try {
      const result = await this.uploadService.uploadPhotoSlot(file, slot);
      this.store.updateSlot(
        slot,
        result.photoObjectKey,
        result.compression.dataUrl,
        result.compression.averageLuminance
      );
      // Keep on current slot so user sees their photo confirmed without screen flashing
      this.activeSlot.set(slot);
    } catch (err: any) {
      this.uploadErrorMessage.set(err.message || 'Falha ao processar e enviar foto');
    } finally {
      this.isUploading.set(false);
    }
  }

  onRemoveSlotPhoto(slot: 'damage' | 'odometer' | 'front' | 'detail', event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.store.clearSlot(slot);
    this.activeSlot.set(slot);
    this.uploadErrorMessage.set(null);
  }

  async onSubmit(): Promise<void> {
    if (!this.canSubmit()) return;

    this.submitErrorMessage.set(null);
    try {
      this.store.reportedContext.set(this.reportedContext());
      this.store.safetyRisk.set(this.safetyRisk());
      this.store.reportedOdometer.set(this.reportedOdometer());
      this.store.riderNotes.set(this.riderNotes());

      const inspectionId = await this.store.submitInspection();
      this.router?.navigate(['/rider/inspections', inspectionId]);
    } catch (err: any) {
      const detail = err.error?.detail || err.message || 'Falha ao registrar vistoria.';
      this.submitErrorMessage.set(detail);
    }
  }

  onBack(): void {
    this.router?.navigate(['/rider/garage']);
  }
}
