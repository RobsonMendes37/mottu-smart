import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom, Subscription } from 'rxjs';
import {
  RiderGarageResponse,
  RiderInspectionDetails,
  YardResponse,
  InspectionStatus,
  ReportedOriginContext,
  SafetyRiskLevel,
  AppointmentShift,
  SseStatusChangeEvent,
} from '../models/rider.models';
import { RiderService } from '../services/rider.service';
import { SseClientService, SseEvent } from '../../../core/services/sse-client.service';
import { generateUuid } from '../../../core/utils/uuid.util';

export interface PhotoSlotState {
  slot: 'damage' | 'odometer' | 'front' | 'detail';
  label: string;
  photoObjectKey: string | null;
  previewUrl: string | null;
  luminance: number | null;
  isDark: boolean;
}

const DEFAULT_SLOTS: PhotoSlotState[] = [
  {
    slot: 'damage',
    label: '1. Foco do Defeito (Obrigatório)',
    photoObjectKey: null,
    previewUrl: null,
    luminance: null,
    isDark: false,
  },
  {
    slot: 'odometer',
    label: '2. Painel / Hodômetro',
    photoObjectKey: null,
    previewUrl: null,
    luminance: null,
    isDark: false,
  },
  {
    slot: 'front',
    label: '3. Frente da Moto',
    photoObjectKey: null,
    previewUrl: null,
    luminance: null,
    isDark: false,
  },
  {
    slot: 'detail',
    label: '4. Detalhe Complementar',
    photoObjectKey: null,
    previewUrl: null,
    luminance: null,
    isDark: false,
  },
];

@Injectable({ providedIn: 'root' })
export class RiderStore {
  private riderService: RiderService;
  private sseService: SseClientService;
  private streamSubscription: Subscription | null = null;

  // Garage State (Screen T02)
  readonly garage = signal<RiderGarageResponse | null>(null);
  readonly isLoadingGarage = signal<boolean>(false);
  readonly garageError = signal<string | null>(null);

  // Wizard State (Screen T03)
  readonly photoSlots = signal<PhotoSlotState[]>([...DEFAULT_SLOTS]);
  readonly reportedContext = signal<ReportedOriginContext>('RECEIVED_LIKE_THIS');
  readonly safetyRisk = signal<SafetyRiskLevel>('SAFE_TO_RIDE');
  readonly reportedOdometer = signal<number>(0);
  readonly riderNotes = signal<string>('');
  readonly isSubmittingInspection = signal<boolean>(false);
  readonly submissionError = signal<string | null>(null);

  // Live Inspection Status State (Screen T04)
  readonly currentInspection = signal<RiderInspectionDetails | null>(null);
  readonly currentLiveStatus = signal<InspectionStatus | null>(null);
  readonly liveStepLabel = signal<string>('Aguardando análise da IA...');
  readonly isStreamingSse = signal<boolean>(false);

  // Yards & Booking
  readonly yards = signal<YardResponse[]>([]);
  readonly isLoadingYards = signal<boolean>(false);
  readonly isBookingAppointment = signal<boolean>(false);
  readonly bookingError = signal<string | null>(null);
  readonly isContesting = signal<boolean>(false);
  readonly contestationError = signal<string | null>(null);

  // Computeds
  readonly activeMotorcycle = computed(() => this.garage()?.activeContract?.motorcycle ?? null);
  readonly openInspectionsCount = computed(() => this.garage()?.openInspectionsCount ?? 0);
  readonly hasReachedQuota = computed(() => this.openInspectionsCount() >= 3);

  readonly primaryPhoto = computed(() => this.photoSlots().find((s) => s.slot === 'damage'));
  readonly isPrimaryPhotoReady = computed(() => !!this.primaryPhoto()?.photoObjectKey);

  readonly isOdometerValid = computed(() => {
    const minOdo = this.garage()?.activeContract?.initialOdometerKm ?? 0;
    return this.reportedOdometer() >= minOdo;
  });

  readonly isWizardValid = computed(() => {
    return this.isPrimaryPhotoReady() && this.isOdometerValid();
  });

  readonly isZeroCostVerdict = computed(() => {
    const verdict = this.currentInspection()?.finalVerdict;
    return verdict === 'PRE_EXISTENTE' || verdict === 'DESGASTE_NATURAL';
  });

  readonly isMauUsoVerdict = computed(() => {
    return this.currentInspection()?.finalVerdict === 'MAU_USO';
  });

  readonly canContestInspection = computed(() => {
    const ins = this.currentInspection();
    return !!ins && ins.canContest && !ins.contestedOnce;
  });

  constructor(riderService?: RiderService, sseService?: SseClientService) {
    try {
      this.riderService = riderService ?? inject(RiderService);
    } catch {
      this.riderService = riderService as RiderService;
    }
    try {
      this.sseService = sseService ?? inject(SseClientService);
    } catch {
      this.sseService = sseService as SseClientService;
    }
  }

  async loadGarage(): Promise<void> {
    this.isLoadingGarage.set(true);
    this.garageError.set(null);
    try {
      const data = await firstValueFrom(this.riderService.getGarage());
      this.garage.set(data);
      if (data.activeContract) {
        this.reportedOdometer.set(data.activeContract.motorcycle.currentOdometerKm);
      }
    } catch (err: any) {
      this.garageError.set(err.message || 'Falha ao carregar dados da garagem');
    } finally {
      this.isLoadingGarage.set(false);
    }
  }

  updateSlot(
    slotName: 'damage' | 'odometer' | 'front' | 'detail',
    key: string,
    previewUrl: string,
    luminance: number
  ): void {
    this.photoSlots.update((slots) =>
      slots.map((s) => {
        if (s.slot === slotName) {
          return {
            ...s,
            photoObjectKey: key,
            previewUrl,
            luminance,
            isDark: luminance < 40,
          };
        }
        return s;
      })
    );
  }

  clearSlot(slotName: 'damage' | 'odometer' | 'front' | 'detail'): void {
    this.photoSlots.update((slots) =>
      slots.map((s) => {
        if (s.slot === slotName) {
          return {
            ...s,
            photoObjectKey: null,
            previewUrl: null,
            luminance: null,
            isDark: false,
          };
        }
        return s;
      })
    );
  }

  resetWizard(): void {
    this.photoSlots.set(DEFAULT_SLOTS.map((s) => ({ ...s })));
    this.reportedContext.set('RECEIVED_LIKE_THIS');
    this.safetyRisk.set('SAFE_TO_RIDE');
    this.riderNotes.set('');
    this.submissionError.set(null);
    if (this.garage()?.activeContract) {
      this.reportedOdometer.set(this.garage()!.activeContract!.motorcycle.currentOdometerKm);
    }
  }

  async submitInspection(): Promise<string> {
    const primaryKey = this.primaryPhoto()?.photoObjectKey;
    if (!primaryKey) {
      throw new Error('A foto do defeito é obrigatória');
    }

    this.isSubmittingInspection.set(true);
    this.submissionError.set(null);
    try {
      const idempotencyKey = generateUuid();
      const response = await firstValueFrom(
        this.riderService.createInspection(idempotencyKey, {
          photoObjectKey: primaryKey,
          reportedContext: this.reportedContext(),
          safetyRisk: this.safetyRisk(),
          reportedOdometerKm: this.reportedOdometer(),
          riderNotes: this.riderNotes().trim() || undefined,
        })
      );
      this.currentLiveStatus.set(response.status);
      return response.inspectionId;
    } catch (err: any) {
      const msg = err.error?.detail || err.message || 'Falha ao enviar vistoria';
      this.submissionError.set(msg);
      throw err;
    } finally {
      this.isSubmittingInspection.set(false);
    }
  }

  async loadInspectionDetails(id: string): Promise<void> {
    try {
      const details = await firstValueFrom(this.riderService.getInspectionDetails(id));
      this.currentInspection.set(details);
      this.currentLiveStatus.set(details.status);
    } catch (err: any) {
      // In offline / mock mode fallback
      console.warn('Failed to fetch inspection details:', err);
    }
  }

  startLiveStream(inspectionId: string): void {
    if (this.streamSubscription) {
      this.streamSubscription.unsubscribe();
    }

    this.isStreamingSse.set(true);
    const streamUrl = `/api/v1/rider/inspections/${inspectionId}/stream`;

    this.streamSubscription = this.sseService
      .connect<SseStatusChangeEvent>(streamUrl)
      .subscribe({
        next: (event: SseEvent<SseStatusChangeEvent>) => {
          if (event.data && typeof event.data === 'object' && 'status' in event.data) {
            this.currentLiveStatus.set(event.data.status);
            this.liveStepLabel.set(event.data.stepLabel || 'Status atualizado');
            if (
              event.data.status === 'AUTO_RESOLVED' ||
              event.data.status === 'HUMAN_RESOLVED' ||
              event.data.status === 'HUMAN_TRIAGE' ||
              event.data.status === 'SCHEDULED' ||
              event.data.status === 'INVALID_IMAGE'
            ) {
              this.loadInspectionDetails(inspectionId);
            }
          }
        },
        error: () => {
          this.isStreamingSse.set(false);
          // Fallback fetch on connection closure
          this.loadInspectionDetails(inspectionId);
        },
        complete: () => {
          this.isStreamingSse.set(false);
          this.loadInspectionDetails(inspectionId);
        },
      });
  }

  stopLiveStream(): void {
    if (this.streamSubscription) {
      this.streamSubscription.unsubscribe();
      this.streamSubscription = null;
    }
    this.isStreamingSse.set(false);
  }

  async loadYards(): Promise<void> {
    this.isLoadingYards.set(true);
    try {
      const list = await firstValueFrom(this.riderService.getYards());
      this.yards.set(list);
    } catch (err: any) {
      console.warn('Failed to load yards:', err);
    } finally {
      this.isLoadingYards.set(false);
    }
  }

  async contestInspection(id: string, reason: string): Promise<void> {
    this.isContesting.set(true);
    this.contestationError.set(null);
    try {
      const res = await firstValueFrom(this.riderService.contestInspection(id, { reason }));
      this.currentLiveStatus.set(res.status);
      await this.loadInspectionDetails(id);
    } catch (err: any) {
      const msg = err.error?.detail || err.message || 'Falha ao solicitar revisão humana';
      this.contestationError.set(msg);
      throw err;
    } finally {
      this.isContesting.set(false);
    }
  }

  async bookAppointment(
    id: string,
    yardId: string,
    scheduledDate: string,
    shift: AppointmentShift
  ): Promise<void> {
    this.isBookingAppointment.set(true);
    this.bookingError.set(null);
    try {
      const res = await firstValueFrom(
        this.riderService.createAppointment(id, {
          yardId,
          scheduledDate,
          shift,
        })
      );
      this.currentLiveStatus.set(res.inspectionStatus);
      await this.loadInspectionDetails(id);
    } catch (err: any) {
      const msg = err.error?.detail || err.message || 'Falha ao agendar visita ao galpão';
      this.bookingError.set(msg);
      throw err;
    } finally {
      this.isBookingAppointment.set(false);
    }
  }
}
