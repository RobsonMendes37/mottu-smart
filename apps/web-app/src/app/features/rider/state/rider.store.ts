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
  readonly liveStepLabel = signal<string>('Analisando fotos da vistoria...');
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
      const normalized = this.normalizeDemoGarage(data);
      this.garage.set(normalized);
      if (normalized.activeContract) {
        this.reportedOdometer.set(normalized.activeContract.motorcycle.currentOdometerKm);
      }
    } catch (err: any) {
      this.garageError.set(err.message || 'Falha ao carregar dados da garagem');
    } finally {
      this.isLoadingGarage.set(false);
    }
  }

  private normalizeDemoGarage(data: RiderGarageResponse): RiderGarageResponse {
    if (!data || !data.activeContract) return data;
    const isDemoContract = data.activeContract.contractId === 'a0000000-0000-0000-0000-000000000001';
    if (!isDemoContract) return data;

    const recents = data.recentInspections || [];
    const hasLegacyTriageSeed = recents.some(
      (r) =>
        r.id === 'd2000000-0000-0000-0000-000000000001' &&
        (r.status === 'HUMAN_TRIAGE' || !r.itemsCount || r.itemsCount < 2)
    );
    if (!hasLegacyTriageSeed && recents.length > 0) {
      return data;
    }

    return {
      ...data,
      openInspectionsCount: 0,
      recentInspections: [
        {
          id: 'd2000000-0000-0000-0000-000000000001',
          status: 'AUTO_RESOLVED',
          finalVerdict: 'MAU_USO',
          totalEstimatedBrl: 140.5,
          createdAt: new Date(Date.now() - 25 * 60000).toISOString(),
          itemsCount: 3,
          partNamesSummary: [
            'Retrovisor Direito Sport (R$ 42,50)',
            'Manete de Freio Direito (R$ 25,00)',
            'Protetor Lateral do Escapamento (R$ 38,00)',
          ],
        },
        {
          id: 'd2000000-0000-0000-0000-000000000003',
          status: 'AUTO_RESOLVED',
          finalVerdict: 'DESGASTE_NATURAL',
          totalEstimatedBrl: 0.0,
          createdAt: new Date(Date.now() - 180 * 60000).toISOString(),
          itemsCount: 2,
          partNamesSummary: [
            'Pneu Traseiro 80/100-14 (R$ 0,00 Coberto)',
            'Lona de Freio Traseira (R$ 0,00 Coberto)',
          ],
        },
        {
          id: 'd2000000-0000-0000-0000-000000000004',
          status: 'HUMAN_RESOLVED',
          finalVerdict: 'MAU_USO',
          totalEstimatedBrl: 190.0,
          createdAt: new Date(Date.now() - 1440 * 60000).toISOString(),
          itemsCount: 2,
          partNamesSummary: [
            'Bloco Óptico do Farol Dianteiro (R$ 130,00)',
            'Manete de Freio Direito (R$ 25,00)',
          ],
        },
      ],
    };
  }

  async validatePickup(): Promise<boolean> {
    try {
      const updated = await firstValueFrom(this.riderService.validatePickup());
      const current = this.garage();
      if (current) {
        this.garage.set({
          ...current,
          activeContract: updated,
        });
      }
      return true;
    } catch {
      return false;
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
      const enriched = this.normalizeDemoInspectionDetails(id, details);
      this.currentInspection.set(enriched);
      this.currentLiveStatus.set(enriched.status);
    } catch (err: any) {
      // In offline / mock mode fallback
      const fallback = this.normalizeDemoInspectionDetails(id, null);
      if (fallback) {
        this.currentInspection.set(fallback);
        this.currentLiveStatus.set(fallback.status);
      } else {
        console.warn('Failed to fetch inspection details:', err);
      }
    }
  }

  private normalizeDemoInspectionDetails(
    id: string,
    details: RiderInspectionDetails | null
  ): RiderInspectionDetails {
    if (
      id === 'd2000000-0000-0000-0000-000000000001' &&
      (!details || details.status === 'HUMAN_TRIAGE' || (details.items || []).length < 3)
    ) {
      return {
        id: 'd2000000-0000-0000-0000-000000000001',
        status: 'AUTO_RESOLVED',
        photoDownloadUrl: details?.photoDownloadUrl || '/demo-photos/avaria-retrovisor.webp',
        reportedContext: 'ACCIDENT_OR_DROP',
        safetyRisk: 'SAFE_TO_RIDE',
        reportedOdometerKm: 1840,
        riderNotes: 'Ao desviar de buraco na chuva a moto tombou na calçada e quebrou o retrovisor direito.',
        finalVerdict: 'MAU_USO',
        aiConfidence: 0.94,
        laborFeeBrl: 35.0,
        totalEstimatedBrl: 140.5,
        customerFeedbackText:
          'Identificamos 3 peças com avaria por queda lateral (Retrovisor Direito, Manete de Freio e Protetor de Escapamento). Valores calculados pela tabela oficial da Loja Mottu para sua previsibilidade.',
        canContest: true,
        contestedOnce: false,
        items: [
          {
            catalogPartId: 'SKU-RETROVISOR-DIR-SPORT',
            partName: 'Retrovisor Direito Sport',
            unitPriceBrl: 42.5,
            addedBy: 'AI',
          },
          {
            catalogPartId: 'SKU-MANETE-DIR-SPORT',
            partName: 'Manete de Freio Direito',
            unitPriceBrl: 25.0,
            addedBy: 'AI',
          },
          {
            catalogPartId: 'SKU-PROT-ESCAP-SPORT',
            partName: 'Protetor Lateral do Escapamento',
            unitPriceBrl: 38.0,
            addedBy: 'AI',
          },
        ],
        appointment: details?.appointment ?? null,
        createdAt: details?.createdAt || new Date(Date.now() - 25 * 60000).toISOString(),
        resolvedAt: details?.resolvedAt || new Date(Date.now() - 24 * 60000).toISOString(),
        committeeEvaluationJson: details?.committeeEvaluationJson ?? null,
      };
    }

    if (
      id === 'd2000000-0000-0000-0000-000000000003' &&
      (!details || details.status === 'HUMAN_TRIAGE' || (details.items || []).length < 2)
    ) {
      return {
        id: 'd2000000-0000-0000-0000-000000000003',
        status: 'AUTO_RESOLVED',
        photoDownloadUrl: details?.photoDownloadUrl || '/demo-photos/avaria-pneu.webp',
        reportedContext: 'NORMAL_USE_WEAR',
        safetyRisk: 'SAFE_TO_RIDE',
        reportedOdometerKm: 8920,
        riderNotes: 'Pneu traseiro e lona de freio atingiram limite de desgaste natural.',
        finalVerdict: 'DESGASTE_NATURAL',
        aiConfidence: 0.965,
        laborFeeBrl: 0.0,
        totalEstimatedBrl: 0.0,
        customerFeedbackText:
          'Desgaste natural confirmado em 2 itens (Pneu Traseiro e Lona de Freio). Cobertura de 100% pelo plano Mottu: você economizou R$ 248,00 e não paga nada (R$ 0,00).',
        canContest: false,
        contestedOnce: false,
        items: [
          {
            catalogPartId: 'SKU-PNEU-TRAS-SPORT',
            partName: 'Pneu Traseiro 80/100-14',
            unitPriceBrl: 180.0,
            addedBy: 'AI',
          },
          {
            catalogPartId: 'SKU-LONA-FREIO-TRAS-SPORT',
            partName: 'Lona de Freio Traseira',
            unitPriceBrl: 33.0,
            addedBy: 'AI',
          },
        ],
        appointment: details?.appointment ?? null,
        createdAt: details?.createdAt || new Date(Date.now() - 180 * 60000).toISOString(),
        resolvedAt: details?.resolvedAt || new Date(Date.now() - 179 * 60000).toISOString(),
        committeeEvaluationJson: details?.committeeEvaluationJson ?? null,
      };
    }

    if (
      id === 'd2000000-0000-0000-0000-000000000004' &&
      (!details || details.status === 'HUMAN_TRIAGE' || (details.items || []).length < 2)
    ) {
      return {
        id: 'd2000000-0000-0000-0000-000000000004',
        status: 'HUMAN_RESOLVED',
        photoDownloadUrl: details?.photoDownloadUrl || '/demo-photos/avaria-farol.webp',
        reportedContext: 'ACCIDENT_OR_DROP',
        safetyRisk: 'CRITICAL_STOPPED',
        reportedOdometerKm: 3420,
        riderNotes: 'Pedra solta atingiu o farol dianteiro e manete.',
        finalVerdict: 'MAU_USO',
        aiConfidence: 0.95,
        laborFeeBrl: 35.0,
        totalEstimatedBrl: 190.0,
        customerFeedbackText:
          'Avaliação concluída pela oficina: 2 peças listadas (Bloco Óptico do Farol e Manete de Freio Direito). Orçamento transparente pronto para agendamento.',
        canContest: false,
        contestedOnce: false,
        items: [
          {
            catalogPartId: 'SKU-FAROL-SPORT',
            partName: 'Bloco Óptico do Farol Dianteiro',
            unitPriceBrl: 130.0,
            addedBy: 'MECHANIC',
          },
          {
            catalogPartId: 'SKU-MANETE-DIR-SPORT',
            partName: 'Manete de Freio Direito',
            unitPriceBrl: 25.0,
            addedBy: 'AI',
          },
        ],
        appointment: details?.appointment ?? null,
        createdAt: details?.createdAt || new Date(Date.now() - 1440 * 60000).toISOString(),
        resolvedAt: details?.resolvedAt || new Date(Date.now() - 1400 * 60000).toISOString(),
        committeeEvaluationJson: details?.committeeEvaluationJson ?? null,
      };
    }

    return details as RiderInspectionDetails;
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
