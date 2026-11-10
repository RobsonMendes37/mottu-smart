import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RiderStore } from '../../state/rider.store';
import { AuthService } from '../../../../core/services/auth.service';
import { AppointmentShift, CommitteeEvaluationData } from '../../models/rider.models';
import { MobileShellComponent } from '../../../../shared/layout/mobile-shell/mobile-shell.component';

@Component({
  selector: 'app-inspection-status',
  standalone: true,
  imports: [CommonModule, FormsModule, MobileShellComponent],
  templateUrl: './inspection-status.component.html',
})
export class InspectionStatusComponent implements OnInit, OnDestroy {
  protected store: RiderStore;
  protected authService: AuthService | null;
  protected router: Router | null;
  protected route: ActivatedRoute | null;

  inspectionId = signal<string>('');

  // Modals state
  isContestModalOpen = signal<boolean>(false);
  contestReason = signal<string>('');

  isAppointmentModalOpen = signal<boolean>(false);
  selectedYardId = signal<string>('');
  selectedDate = signal<string>('');
  selectedShift = signal<AppointmentShift>('MORNING');

  todayDate = new Date().toISOString().split('T')[0];

  // Store bindings
  inspection = computed(() => {
    const fn = this.store.currentInspection;
    return typeof fn === 'function' ? fn() : null;
  });

  status = computed(() => {
    const live = this.store.currentLiveStatus;
    const current = typeof live === 'function' ? live() : null;
    return current || this.inspection()?.status || 'QUEUED_AI';
  });

  isZeroCost = computed(() => {
    const ins = this.inspection();
    if (!ins) return false;
    const v = ins.finalVerdict;
    return v === 'PRE_EXISTENTE' || v === 'DESGASTE_NATURAL' || ins.totalEstimatedBrl === 0;
  });

  isMauUso = computed(() => {
    return this.inspection()?.finalVerdict === 'MAU_USO';
  });

  isTriage = computed(() => {
    return this.status() === 'HUMAN_TRIAGE';
  });

  isScheduled = computed(() => {
    return this.status() === 'SCHEDULED' || !!this.inspection()?.appointment;
  });

  isInvalid = computed(() => {
    return this.status() === 'INVALID_IMAGE';
  });

  canContest = computed(() => {
    const ins = this.inspection();
    return !!ins && ins.canContest && !ins.contestedOnce;
  });

  contestedOnce = computed(() => {
    return this.inspection()?.contestedOnce ?? false;
  });

  committeeEvaluation = computed<CommitteeEvaluationData | null>(() => {
    const raw = this.inspection()?.committeeEvaluationJson;
    if (!raw) return null;
    try {
      return typeof raw === 'string' ? JSON.parse(raw) : (raw as unknown as CommitteeEvaluationData);
    } catch {
      return null;
    }
  });

  isPresentialRequested = computed(() => {
    return !!this.inspection()?.presentialInspectionRequested;
  });

  presentialReason = computed(() => {
    return this.inspection()?.presentialRequestReason || 'Convocação para avaliação técnica detalhada na oficina.';
  });

  hasLowPhotoClarity = computed(() => {
    const c = this.committeeEvaluation();
    return c ? !c.canIdentifyPartsClearly : false;
  });

  totalPriceDisplay = computed(() => {
    const ins = this.inspection();
    if (!ins) return 'Calculando...';
    if (this.isZeroCost()) return 'R$ 0,00';
    if (this.isTriage()) return 'Sob Avaliação';
    if (this.isInvalid()) return 'R$ 0,00';
    return `R$ ${ins.totalEstimatedBrl.toFixed(2).replace('.', ',')}`;
  });

  isContestReasonValid = computed(() => {
    return this.contestReason().trim().length >= 10;
  });

  constructor(
    store?: RiderStore,
    authService?: AuthService,
    router?: Router,
    route?: ActivatedRoute
  ) {
    try {
      this.store = store ?? inject(RiderStore);
    } catch {
      this.store = store as RiderStore;
    }
    try {
      this.authService = authService ?? inject(AuthService, { optional: true });
    } catch {
      this.authService = authService ?? null;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
    try {
      this.route = route ?? inject(ActivatedRoute, { optional: true });
    } catch {
      this.route = route ?? null;
    }
  }

  ngOnInit(): void {
    const id = this.route?.snapshot?.paramMap?.get('id') || '';
    if (id) {
      this.inspectionId.set(id);
      this.store.loadInspectionDetails(id);
      this.store.startLiveStream(id);
    }
    this.store.loadYards();
  }

  ngOnDestroy(): void {
    this.store.stopLiveStream();
  }

  openContestModal(): void {
    this.contestReason.set('');
    this.isContestModalOpen.set(true);
  }

  closeContestModal(): void {
    this.isContestModalOpen.set(false);
  }

  async onSubmitContestation(): Promise<void> {
    if (!this.isContestReasonValid()) return;
    try {
      await this.store.contestInspection(this.inspectionId(), this.contestReason().trim());
      this.closeContestModal();
    } catch (err: any) {
      console.warn('Contestation failed:', err);
    }
  }

  openAppointmentModal(): void {
    const defaultYard = this.store.yards()[0]?.id || '';
    this.selectedYardId.set(defaultYard);
    this.selectedDate.set(this.todayDate);
    this.selectedShift.set('MORNING');
    this.isAppointmentModalOpen.set(true);
  }

  closeAppointmentModal(): void {
    this.isAppointmentModalOpen.set(false);
  }

  async onSubmitAppointment(): Promise<void> {
    if (!this.selectedYardId() || !this.selectedDate()) return;
    try {
      await this.store.bookAppointment(
        this.inspectionId(),
        this.selectedYardId(),
        this.selectedDate(),
        this.selectedShift()
      );
      this.closeAppointmentModal();
    } catch (err: any) {
      console.warn('Booking failed:', err);
    }
  }

  formatFeedback(text: string | null | undefined): string {
    if (!text) return '';
    return text
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*(.*?)\*/g, '$1')
      .replace(/^#{1,6}\s+/gm, '')
      .replace(/^\s*[-*]\s+/gm, '• ')
      .trim();
  }

  onBack(): void {
    this.router?.navigate(['/rider/garage']);
  }

  onNewPhoto(): void {
    this.router?.navigate(['/rider/inspections/new']);
  }
}
