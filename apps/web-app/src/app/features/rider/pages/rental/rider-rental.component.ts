import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MobileShellComponent, RiderTabKey } from '../../../../shared/layout/mobile-shell/mobile-shell.component';
import { RiderStore } from '../../state/rider.store';
import { RiderService } from '../../services/rider.service';
import { DailyDeliverySummary, RiderRentalSummaryDto } from '../../models/rider.models';

export type { DailyDeliverySummary };

@Component({
  selector: 'app-rider-rental',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MobileShellComponent],
  templateUrl: './rider-rental.component.html',
})
export class RiderRentalComponent implements OnInit {
  protected riderStore: RiderStore;
  private route: ActivatedRoute | null;
  private riderService: RiderService | null;

  readonly baseWeeklyPlanBrl = 196.0;
  readonly punctualityDiscountBrl = 15.0;
  readonly pixPayload =
    '00020126580014BR.GOV.BCB.PIX0136mottu-ops-fatura-semanal-sp04-20265204000053039865802BR';

  activeSection = signal<'rental' | 'deliveries'>('rental');
  pixCopied = signal<boolean>(false);
  paymentStatus = signal<string>('PENDING_PIX');
  dueDateLabel = signal<string>('Vence segunda-feira • 18:00');
  basePlanBrl = signal<number>(196.0);
  discountBrl = signal<number>(15.0);
  deliveryBonusBrl = signal<number>(0.0);
  contractCode = signal<string>('CTR-2026-8841');
  feedbackMessage = signal<string | null>(null);

  // Deliveries registration
  isRegisterDeliveriesOpen = signal<boolean>(false);
  newDeliveriesCount = signal<number>(5);
  newDeliveriesEarnings = signal<number>(52.5);
  isSubmittingDeliveries = signal<boolean>(false);

  deliveryDaysSignal = signal<DailyDeliverySummary[]>([
    { dayLabel: 'Hoje • Sexta', completedCount: 24, earningsBrl: 238.5, onTimeRate: '100%' },
    { dayLabel: 'Ontem • Quinta', completedCount: 26, earningsBrl: 254.0, onTimeRate: '96%' },
    { dayLabel: 'Quarta-feira', completedCount: 22, earningsBrl: 210.0, onTimeRate: '100%' },
    { dayLabel: 'Terça-feira', completedCount: 25, earningsBrl: 242.0, onTimeRate: '98%' },
    { dayLabel: 'Segunda-feira', completedCount: 21, earningsBrl: 198.0, onTimeRate: '98%' },
  ]);

  get deliveryDays(): DailyDeliverySummary[] {
    return this.deliveryDaysSignal();
  }

  shellActiveTab = computed<RiderTabKey>(() =>
    this.activeSection() === 'deliveries' ? 'deliveries' : 'rental'
  );

  activePlate = computed(() => {
    const fn = this.riderStore?.activeMotorcycle;
    const bike = typeof fn === 'function' ? fn() : null;
    return bike?.plate || 'MOT1A23';
  });

  chargedInspections = computed(() => {
    const g = this.riderStore?.garage?.();
    const list = g?.recentInspections || [];
    return list.filter((i) => i.finalVerdict === 'MAU_USO' && i.totalEstimatedBrl > 0);
  });

  inspectionChargesBrl = computed(() => {
    return this.chargedInspections().reduce((sum, item) => sum + Number(item.totalEstimatedBrl), 0);
  });

  weeklyTotalBrl = computed(() => {
    const total =
      this.basePlanBrl() -
      this.discountBrl() -
      this.deliveryBonusBrl() +
      this.inspectionChargesBrl();
    return Math.max(0, total);
  });

  weeklyDeliveriesTotal = computed(() =>
    this.deliveryDaysSignal().reduce((acc, d) => acc + d.completedCount, 0)
  );

  weeklyDeliveryEarningsBrl = computed(() =>
    this.deliveryDaysSignal().reduce((acc, d) => acc + d.earningsBrl, 0)
  );

  isGoalAchieved = computed(() => this.weeklyDeliveriesTotal() >= 120);

  constructor(
    riderStore?: RiderStore,
    route?: ActivatedRoute,
    riderService?: RiderService
  ) {
    try {
      this.riderStore = riderStore ?? inject(RiderStore);
    } catch {
      this.riderStore = riderStore as RiderStore;
    }
    try {
      this.route = route ?? inject(ActivatedRoute, { optional: true });
    } catch {
      this.route = route ?? null;
    }
    try {
      this.riderService = riderService ?? inject(RiderService, { optional: true });
    } catch {
      this.riderService = riderService ?? null;
    }
  }

  ngOnInit(): void {
    const initialTab = this.route?.snapshot?.queryParamMap?.get('tab');
    if (initialTab === 'deliveries') {
      this.activeSection.set('deliveries');
    }

    this.route?.queryParamMap?.subscribe?.((params) => {
      const tab = params.get('tab');
      this.activeSection.set(tab === 'deliveries' ? 'deliveries' : 'rental');
    });

    if (this.riderStore?.garage && !this.riderStore.garage()) {
      this.riderStore.loadGarage?.();
    }

    this.loadRentalSummary();
  }

  loadRentalSummary(): void {
    if (!this.riderService) return;

    this.riderService.getRentalSummary().subscribe({
      next: (summary: RiderRentalSummaryDto) => {
        if (!summary) return;
        this.contractCode.set(summary.contractCode);
        this.basePlanBrl.set(summary.baseWeeklyPlanBrl);
        this.discountBrl.set(summary.punctualityDiscountBrl);
        this.deliveryBonusBrl.set(summary.deliveryBonusBrl);
        this.paymentStatus.set(summary.paymentStatus);
        this.dueDateLabel.set(summary.dueDateLabel);
        if (summary.deliveryDays && summary.deliveryDays.length > 0) {
          this.deliveryDaysSignal.set(summary.deliveryDays);
        }
      },
      error: () => {},
    });
  }

  setSection(section: 'rental' | 'deliveries'): void {
    this.activeSection.set(section);
  }

  async copyPixCode(): Promise<void> {
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(this.pixPayload);
      }
    } catch {
      // Clipboard fallback in restricted contexts
    }
    this.pixCopied.set(true);
    setTimeout(() => this.pixCopied.set(false), 3500);
  }

  payPix(): void {
    this.paymentStatus.set('PAID');
    this.dueDateLabel.set('Pago via Pix • Confirmado');
    this.feedbackMessage.set('Pagamento Pix confirmado com sucesso!');

    if (this.riderService) {
      this.riderService.payRentalPix().subscribe({
        next: (summary) => {
          this.paymentStatus.set(summary.paymentStatus);
          this.dueDateLabel.set(summary.dueDateLabel);
        },
        error: () => {},
      });
    }
  }

  toggleRegisterDeliveries(): void {
    this.isRegisterDeliveriesOpen.update((v) => !v);
  }

  submitDeliveries(): void {
    const count = Number(this.newDeliveriesCount());
    const earnings = Number(this.newDeliveriesEarnings());
    if (count <= 0) return;

    this.isSubmittingDeliveries.set(true);

    const updateLocalDeliveries = () => {
      const currentList = this.deliveryDaysSignal();
      const today = currentList[0];
      const updatedToday: DailyDeliverySummary = {
        dayLabel: today.dayLabel,
        completedCount: today.completedCount + count,
        earningsBrl: today.earningsBrl + earnings,
        onTimeRate: today.onTimeRate,
      };
      const updatedList = [updatedToday, ...currentList.slice(1)];
      this.deliveryDaysSignal.set(updatedList);

      const total = updatedList.reduce((acc, d) => acc + d.completedCount, 0);
      if (total >= 120) {
        this.deliveryBonusBrl.set(40.0);
        this.feedbackMessage.set(
          `Parabéns! Meta de 120 entregas batida (${total} entregas). R$ 40,00 de bônus creditados no aluguel!`
        );
      } else {
        this.feedbackMessage.set(
          `+${count} entregas registradas com sucesso! Total semanal: ${total}/120.`
        );
      }
      this.isSubmittingDeliveries.set(false);
      this.isRegisterDeliveriesOpen.set(false);
    };

    if (this.riderService) {
      this.riderService.registerDailyDeliveries(count, earnings).subscribe({
        next: (summary) => {
          if (summary.deliveryDays && summary.deliveryDays.length > 0) {
            this.deliveryDaysSignal.set(summary.deliveryDays);
          }
          this.deliveryBonusBrl.set(summary.deliveryBonusBrl);
          if (summary.weeklyDeliveriesTotal >= 120) {
            this.feedbackMessage.set(
              `Parabéns! Meta de 120 entregas batida (${summary.weeklyDeliveriesTotal} entregas). R$ 40,00 de bônus creditados no aluguel!`
            );
          } else {
            this.feedbackMessage.set(
              `+${count} entregas registradas com sucesso! Total semanal: ${summary.weeklyDeliveriesTotal}/120.`
            );
          }
          this.isSubmittingDeliveries.set(false);
          this.isRegisterDeliveriesOpen.set(false);
        },
        error: () => {
          updateLocalDeliveries();
        },
      });
    } else {
      updateLocalDeliveries();
    }
  }
}
