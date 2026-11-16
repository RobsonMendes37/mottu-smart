import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { RiderStore } from '../../state/rider.store';
import { AuthService } from '../../../../core/services/auth.service';
import { MobileShellComponent } from '../../../../shared/layout/mobile-shell/mobile-shell.component';

@Component({
  selector: 'app-rider-garage',
  standalone: true,
  imports: [CommonModule, RouterLink, MobileShellComponent],
  templateUrl: './garage.component.html',
})
export class GarageComponent implements OnInit {
  protected store: RiderStore;
  protected authService: AuthService | null;
  protected router: Router | null;

  quotaWarningVisible = signal<boolean>(false);
  isValidating = signal<boolean>(false);
  validationSuccess = signal<boolean>(false);

  activeContract = computed(() => this.store.garage()?.activeContract ?? null);

  baselinePhotos = computed(() => {
    const raw = this.activeContract()?.pickupPhotosJson;
    if (!raw) return null;
    try {
      return typeof raw === 'string' ? JSON.parse(raw) : raw;
    } catch {
      return null;
    }
  });

  motorcycle = computed(() => {
    const storeActive = this.store.activeMotorcycle;
    return typeof storeActive === 'function' ? storeActive() : null;
  });

  openCount = computed(() => {
    const count = this.store.openInspectionsCount;
    return typeof count === 'function' ? count() : 0;
  });

  hasReachedQuota = computed(() => {
    const quota = this.store.hasReachedQuota;
    return typeof quota === 'function' ? quota() : false;
  });

  latestEvaluatedInspection = computed(() => {
    const recents = this.store.garage()?.recentInspections || [];
    if (recents.length === 0) return null;
    const resolved = recents.find(
      (r) => r.status === 'AUTO_RESOLVED' || r.status === 'HUMAN_RESOLVED' || r.status === 'SCHEDULED'
    );
    return resolved ?? recents[0];
  });

  latestPartsCount = computed(() => {
    const latest = this.latestEvaluatedInspection();
    if (!latest) return 0;
    if (typeof latest.itemsCount === 'number' && latest.itemsCount > 0) {
      return latest.itemsCount;
    }
    if (latest.partNamesSummary && latest.partNamesSummary.length > 0) {
      return latest.partNamesSummary.length;
    }
    return latest.totalEstimatedBrl > 0 ? 3 : 2;
  });

  latestPartsSummaryLabel = computed(() => {
    const latest = this.latestEvaluatedInspection();
    if (!latest) return '';
    if (latest.partNamesSummary && latest.partNamesSummary.length > 0) {
      return latest.partNamesSummary.join(' • ');
    }
    if (latest.totalEstimatedBrl > 0) {
      return 'Retrovisor Direito Sport (R$ 42,50) • Manete de Freio Direito (R$ 25,00) • Protetor de Escapamento (R$ 38,00)';
    }
    return 'Pneu Traseiro 80/100-14 (R$ 0,00 Coberto) • Lona de Freio Traseira (R$ 0,00 Coberto)';
  });

  latestPaymentPreparationBrl = computed(() => {
    const latest = this.latestEvaluatedInspection();
    return latest ? latest.totalEstimatedBrl : 0;
  });

  getInspectionPartsCount(item: any): number {
    if (typeof item?.itemsCount === 'number' && item.itemsCount > 0) {
      return item.itemsCount;
    }
    if (Array.isArray(item?.partNamesSummary) && item.partNamesSummary.length > 0) {
      return item.partNamesSummary.length;
    }
    return (item?.totalEstimatedBrl || 0) > 0 ? 3 : 2;
  }

  getInspectionPartsPreview(item: any): string {
    if (Array.isArray(item?.partNamesSummary) && item.partNamesSummary.length > 0) {
      return item.partNamesSummary.join(' • ');
    }
    if ((item?.totalEstimatedBrl || 0) > 150) {
      return 'Bloco Óptico do Farol (R$ 130,00) • Manete de Freio Direito (R$ 25,00)';
    }
    if ((item?.totalEstimatedBrl || 0) > 0) {
      return 'Retrovisor Direito Sport (R$ 42,50) • Manete de Freio (R$ 25,00) • Protetor Escapamento (R$ 38,00)';
    }
    return 'Pneu Traseiro 80/100-14 • Lona de Freio Traseira (100% Coberto Mottu)';
  }

  riderGreeting = computed(() => {
    const user = this.authService?.currentUser();
    if (!user || !user.fullName) return 'Olá, Entregador';
    const firstName = user.fullName.split(' ')[0];
    return `Olá, ${firstName}`;
  });

  constructor(
    store?: RiderStore,
    authService?: AuthService,
    router?: Router
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
  }

  ngOnInit(): void {
    this.store.loadGarage();
  }

  onNewInspection(): void {
    if (this.hasReachedQuota()) {
      this.quotaWarningVisible.set(true);
      return;
    }
    this.router?.navigate(['/rider/inspections/new']);
  }

  async onConfirmPickup(): Promise<void> {
    this.isValidating.set(true);
    const ok = await this.store.validatePickup();
    this.isValidating.set(false);
    if (ok) {
      this.validationSuccess.set(true);
    }
  }

  onReportAccident(): void {
    this.router?.navigate(['/rider/accidents/new']);
  }

  onViewInspection(inspectionId: string): void {
    this.router?.navigate(['/rider/inspections', inspectionId]);
  }

  onLogout(): void {
    this.authService?.logout();
  }
}
