import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';
import { ManagerService } from '../../services/manager.service';
import {
  ContractPaymentStatus,
  WeeklyBillingContract,
} from '../../models/manager.models';

export type { ContractPaymentStatus, WeeklyBillingContract };

@Component({
  selector: 'app-t12-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, DesktopShellComponent],
  templateUrl: './t12-billing.component.html',
})
export class T12BillingComponent implements OnInit {
  private managerService: ManagerService | null;

  activeFilter = signal<'ALL' | ContractPaymentStatus>('ALL');
  searchQuery = signal<string>('');
  selectedContract = signal<WeeklyBillingContract | null>(null);
  feedbackBanner = signal<string | null>(null);

  contracts = signal<WeeklyBillingContract[]>([
    {
      contractCode: 'CTR-2026-8841',
      riderName: 'Carlos Silva',
      plate: 'MOT1A23',
      model: 'Mottu Sport 110i',
      weeklyPlanBrl: 196.0,
      inspectionChargesBrl: 60.0,
      discountBrl: 15.0,
      paymentStatus: 'PAID',
      dueDateLabel: 'Pago via Pix • Seg 08:14',
    },
    {
      contractCode: 'CTR-2026-8854',
      riderName: 'Marcos Souza',
      plate: 'MOT8K11',
      model: 'Mottu Sport 110i',
      weeklyPlanBrl: 196.0,
      inspectionChargesBrl: 102.5,
      discountBrl: 0.0,
      paymentStatus: 'PENDING_PIX',
      dueDateLabel: 'Vence hoje • 18:00',
    },
    {
      contractCode: 'CTR-2026-8790',
      riderName: 'Lucas Lima',
      plate: 'POP3C45',
      model: 'Honda Pop 110i',
      weeklyPlanBrl: 210.0,
      inspectionChargesBrl: 0.0,
      discountBrl: 15.0,
      paymentStatus: 'PAID',
      dueDateLabel: 'Pago via Pix • Dom 21:40',
    },
    {
      contractCode: 'CTR-2026-8612',
      riderName: 'Diego Nascimento',
      plate: 'TVS4D56',
      model: 'TVS Apache 160',
      weeklyPlanBrl: 238.0,
      inspectionChargesBrl: 42.5,
      discountBrl: 0.0,
      paymentStatus: 'OVERDUE',
      dueDateLabel: 'Em atraso há 2 dias',
    },
  ]);

  filteredContracts = computed(() => {
    const status = this.activeFilter();
    const q = this.searchQuery().trim().toLowerCase();

    return this.contracts().filter((c) => {
      const matchesStatus = status === 'ALL' || c.paymentStatus === status;
      const matchesQuery =
        !q ||
        c.contractCode.toLowerCase().includes(q) ||
        c.riderName.toLowerCase().includes(q) ||
        c.plate.toLowerCase().includes(q);
      return matchesStatus && matchesQuery;
    });
  });

  totalWeeklyRevenueBrl = computed(() =>
    this.contracts().reduce((sum, c) => sum + this.netTotal(c), 0)
  );

  paidCount = computed(() => this.contracts().filter((c) => c.paymentStatus === 'PAID').length);

  withInspectionChargeCount = computed(
    () => this.contracts().filter((c) => c.inspectionChargesBrl > 0).length
  );

  constructor(managerService?: ManagerService) {
    try {
      this.managerService = managerService ?? inject(ManagerService, { optional: true });
    } catch {
      this.managerService = managerService ?? null;
    }
  }

  ngOnInit(): void {
    this.loadContracts();
  }

  loadContracts(): void {
    if (!this.managerService) return;

    this.managerService.getBillingContracts().subscribe({
      next: (list) => {
        if (list && list.length > 0) {
          this.contracts.set(list);
        }
      },
      error: () => {},
    });
  }

  netTotal(c: WeeklyBillingContract): number {
    return c.weeklyPlanBrl + c.inspectionChargesBrl - c.discountBrl;
  }

  setFilter(filter: 'ALL' | ContractPaymentStatus): void {
    this.activeFilter.set(filter);
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  openDrawer(contract: WeeklyBillingContract): void {
    this.selectedContract.set(contract);
  }

  closeDrawer(): void {
    this.selectedContract.set(null);
  }

  confirmPix(contractCode: string): void {
    // Otimista
    this.contracts.update((list) =>
      list.map((c) =>
        c.contractCode === contractCode
          ? { ...c, paymentStatus: 'PAID', dueDateLabel: 'Pago via Pix • Confirmado' }
          : c
      )
    );

    const sel = this.selectedContract();
    if (sel && sel.contractCode === contractCode) {
      this.selectedContract.set({
        ...sel,
        paymentStatus: 'PAID',
        dueDateLabel: 'Pago via Pix • Confirmado',
      });
    }

    this.feedbackBanner.set(
      `Pagamento Pix do contrato ${contractCode} confirmado com sucesso!`
    );

    if (this.managerService) {
      this.managerService.confirmPixPayment(contractCode).subscribe({
        next: (updated) => {
          this.contracts.update((list) =>
            list.map((c) => (c.contractCode === contractCode ? updated : c))
          );
          if (this.selectedContract()?.contractCode === contractCode) {
            this.selectedContract.set(updated);
          }
        },
        error: () => {},
      });
    }
  }
}
