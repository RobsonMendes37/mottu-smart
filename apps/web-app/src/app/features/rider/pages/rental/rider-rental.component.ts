import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MobileShellComponent, RiderTabKey } from '../../../../shared/layout/mobile-shell/mobile-shell.component';
import { RiderStore } from '../../state/rider.store';

export interface DailyDeliverySummary {
  dayLabel: string;
  completedCount: number;
  earningsBrl: number;
  onTimeRate: string;
}

@Component({
  selector: 'app-rider-rental',
  standalone: true,
  imports: [CommonModule, RouterLink, MobileShellComponent],
  templateUrl: './rider-rental.component.html',
})
export class RiderRentalComponent implements OnInit {
  protected riderStore: RiderStore;
  private route: ActivatedRoute | null;

  readonly baseWeeklyPlanBrl = 196.0;
  readonly punctualityDiscountBrl = 15.0;
  readonly pixPayload =
    '00020126580014BR.GOV.BCB.PIX0136mottu-ops-fatura-semanal-sp04-20265204000053039865802BR';

  activeSection = signal<'rental' | 'deliveries'>('rental');
  pixCopied = signal<boolean>(false);

  readonly deliveryDays: DailyDeliverySummary[] = [
    { dayLabel: 'Hoje • Sexta', completedCount: 24, earningsBrl: 238.5, onTimeRate: '100%' },
    { dayLabel: 'Ontem • Quinta', completedCount: 26, earningsBrl: 254.0, onTimeRate: '96%' },
    { dayLabel: 'Quarta-feira', completedCount: 22, earningsBrl: 210.0, onTimeRate: '100%' },
    { dayLabel: 'Terça-feira', completedCount: 25, earningsBrl: 242.0, onTimeRate: '98%' },
    { dayLabel: 'Segunda-feira', completedCount: 21, earningsBrl: 198.0, onTimeRate: '98%' },
  ];

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
    return this.baseWeeklyPlanBrl - this.punctualityDiscountBrl + this.inspectionChargesBrl();
  });

  weeklyDeliveriesTotal = computed(() =>
    this.deliveryDays.reduce((acc, d) => acc + d.completedCount, 0)
  );

  weeklyDeliveryEarningsBrl = computed(() =>
    this.deliveryDays.reduce((acc, d) => acc + d.earningsBrl, 0)
  );

  constructor(riderStore?: RiderStore, route?: ActivatedRoute) {
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
}

