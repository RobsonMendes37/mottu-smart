import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MechanicService } from '../../services/mechanic.service';
import {
  MechanicQueueItemDto,
  MotorcycleModel,
  QueuePriorityReason,
} from '../../models/mechanic.models';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';

export interface PriorityBadgeInfo {
  label: string;
  colorClass: string;
  badgeBg: string;
  borderColor: string;
  dotColor: string;
  actionText: string;
}

@Component({
  selector: 'app-t05-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, DesktopShellComponent],
  templateUrl: './t05-queue.component.html',
})
export class T05QueueComponent implements OnInit {
  private mechanicService: MechanicService;
  private router: Router;

  queueItems = signal<MechanicQueueItemDto[]>([]);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  selectedReason = signal<QueuePriorityReason | ''>('');
  searchPlate = signal<string>('');
  aiDeflectionRate = signal<number>(78.5);
  totalElements = signal<number>(0);
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);

  criticalCount = computed(
    () => this.queueItems().filter((i) => i.priorityReason === 'CRITICAL_SAFETY').length
  );
  contestationCount = computed(
    () => this.queueItems().filter((i) => i.priorityReason === 'RIDER_CONTESTATION').length
  );

  constructor(mechanicService?: MechanicService, router?: Router) {
    try {
      this.mechanicService = mechanicService ?? inject(MechanicService);
    } catch {
      this.mechanicService = mechanicService as MechanicService;
    }
    try {
      this.router = router ?? inject(Router);
    } catch {
      this.router = router as Router;
    }
  }

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const reason = this.selectedReason() || undefined;
    const plate = this.searchPlate();

    this.mechanicService
      .getQueue(reason, plate, this.currentPage(), this.pageSize())
      .subscribe({
        next: (page) => {
          this.queueItems.set(page.items);
          this.totalElements.set(page.totalElements);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Não foi possível carregar a fila da oficina.');
          this.isLoading.set(false);
        },
      });
  }

  setReason(reason: QueuePriorityReason | ''): void {
    this.selectedReason.set(reason);
    this.currentPage.set(0);
    this.loadQueue();
  }

  onSearchPlate(plate: string): void {
    this.searchPlate.set(plate.trim().toUpperCase());
    this.currentPage.set(0);
    this.loadQueue();
  }

  openWorkbench(inspectionId: string): void {
    this.router.navigate(['/mechanic/workbench', inspectionId]);
  }

  getPriorityBadge(reason: QueuePriorityReason, _priorityWeight: number): PriorityBadgeInfo {
    switch (reason) {
      case 'CRITICAL_SAFETY':
        return {
          label: 'P1 • Moto parada / segurança',
          colorClass: 'text-[#B91C1C]',
          badgeBg: 'bg-[#FEE2E2]',
          borderColor: 'border-[#DC2626]/30',
          dotColor: 'bg-[#DC2626]',
          actionText: 'Avaliar agora',
        };
      case 'RIDER_CONTESTATION':
        return {
          label: 'P2 • Pedido de revisão',
          colorClass: 'text-[#92400E]',
          badgeBg: 'bg-[#FEF3C7]',
          borderColor: 'border-[#D97706]/30',
          dotColor: 'bg-[#D97706]',
          actionText: 'Revisar pedido',
        };
      case 'LOW_AI_CONFIDENCE':
        return {
          label: 'P3 • Conferência visual',
          colorClass: 'text-[#92400E]',
          badgeBg: 'bg-[#FEF3C7]',
          borderColor: 'border-[#D97706]/30',
          dotColor: 'bg-[#D97706]',
          actionText: 'Abrir bancada',
        };
      case 'FRAUD_SUSPICION':
      default:
        return {
          label: 'P4 • Foto repetida',
          colorClass: 'text-[#334155]',
          badgeBg: 'bg-[#EEF2F6]',
          borderColor: 'border-[#334155]/25',
          dotColor: 'bg-[#0284C7]',
          actionText: 'Conferir foto',
        };
    }
  }

  formatModel(model: MotorcycleModel | string): string {
    switch (model) {
      case 'MOTTU_SPORT_110I':
        return 'Mottu Sport 110i';
      case 'HONDA_POP_110I':
        return 'Honda Pop 110i';
      case 'TVS_APACHE_160':
        return 'TVS Apache 160';
      case 'TVS_RAIDER_125':
        return 'TVS Raider 125';
      default:
        return model || 'Mottu Sport';
    }
  }

  formatVerdict(verdict: string): string {
    switch (verdict) {
      case 'PRE_EXISTENTE':
        return 'Pré-existente';
      case 'DESGASTE_NATURAL':
        return 'Desgaste Natural';
      case 'MAU_USO':
        return 'Mau Uso';
      default:
        return verdict;
    }
  }
}
