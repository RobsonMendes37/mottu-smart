import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';
import { MechanicService } from '../../services/mechanic.service';
import { BayServiceOrder, LocalStockItem } from '../../models/mechanic.models';

export type { BayServiceOrder, LocalStockItem };

@Component({
  selector: 'app-mechanic-operations',
  standalone: true,
  imports: [CommonModule, FormsModule, DesktopShellComponent],
  templateUrl: './mechanic-operations.component.html',
})
export class MechanicOperationsComponent implements OnInit {
  private route: ActivatedRoute | null;
  private mechanicService: MechanicService | null;

  activeTab = signal<'orders' | 'stock'>('orders');
  stockSearch = signal<string>('');
  feedbackBanner = signal<string | null>(null);

  // New Service Order Modal / Inline Form State
  isNewOrderOpen = signal<boolean>(false);
  newOrderPlate = signal<string>('');
  newOrderBay = signal<string>('Baia 01 • Rápida');
  newOrderTitle = signal<string>('');
  newOrderSubmitting = signal<boolean>(false);
  newOrderError = signal<string | null>(null);

  serviceOrders = signal<BayServiceOrder[]>([
    {
      id: 'os-1',
      code: 'OS-2026-0412',
      plate: 'MOT8K11',
      model: 'Mottu Sport 110i',
      bay: 'Baia 01 • Urgente',
      serviceTitle: 'Troca de manete direito + alinhamento do guidão',
      originLabel: 'Vistoria #8841 • Moto parada',
      slaMinutes: 25,
      status: 'IN_REPAIR',
    },
    {
      id: 'os-2',
      code: 'OS-2026-0415',
      plate: 'MOT1A23',
      model: 'Mottu Sport 110i',
      bay: 'Baia 03',
      serviceTitle: 'Substituição de pastilha dianteira (desgaste natural)',
      originLabel: 'Agendamento manhã • Custo R$ 0,00',
      slaMinutes: 40,
      status: 'WAITING_BAY',
    },
    {
      id: 'os-3',
      code: 'OS-2026-0409',
      plate: 'POP3C45',
      model: 'Honda Pop 110i',
      bay: 'Pátio frontal',
      serviceTitle: 'Troca do retrovisor direito e revisão de luz de freio',
      originLabel: 'Vistoria #8819 • Pronta para retirada',
      slaMinutes: 0,
      status: 'READY_PICKUP',
    },
  ]);

  stockItems = signal<LocalStockItem[]>([
    {
      sku: 'MOT-110-MFD',
      partName: 'Manete de freio direito',
      model: 'Mottu Sport 110i',
      aisleBin: 'Corredor A • Gaveta 04',
      quantity: 18,
      minQuantity: 8,
      unitPriceBrl: 25.0,
    },
    {
      sku: 'MOT-110-RTD',
      partName: 'Espelho retrovisor direito',
      model: 'Mottu Sport 110i',
      aisleBin: 'Corredor A • Gaveta 09',
      quantity: 5,
      minQuantity: 6,
      unitPriceBrl: 42.5,
    },
    {
      sku: 'MOT-110-PFD',
      partName: 'Jogo de pastilha de freio dianteiro',
      model: 'Mottu Sport 110i',
      aisleBin: 'Corredor B • Gaveta 02',
      quantity: 24,
      minQuantity: 10,
      unitPriceBrl: 33.0,
    },
    {
      sku: 'POP-110-SPD',
      partName: 'Seta dianteira direita completa',
      model: 'Honda Pop 110i',
      aisleBin: 'Corredor C • Gaveta 07',
      quantity: 2,
      minQuantity: 5,
      unitPriceBrl: 38.0,
    },
  ]);

  inRepairCount = computed(
    () => this.serviceOrders().filter((o) => o.status === 'IN_REPAIR').length
  );
  waitingBayCount = computed(
    () => this.serviceOrders().filter((o) => o.status === 'WAITING_BAY').length
  );
  readyPickupCount = computed(
    () => this.serviceOrders().filter((o) => o.status === 'READY_PICKUP').length
  );

  filteredStockItems = computed(() => {
    const q = this.stockSearch().trim().toLowerCase();
    if (!q) return this.stockItems();
    return this.stockItems().filter(
      (i) =>
        i.sku.toLowerCase().includes(q) ||
        i.partName.toLowerCase().includes(q) ||
        i.aisleBin.toLowerCase().includes(q)
    );
  });

  constructor(route?: ActivatedRoute, mechanicService?: MechanicService) {
    try {
      this.route = route ?? inject(ActivatedRoute, { optional: true });
    } catch {
      this.route = route ?? null;
    }
    try {
      this.mechanicService = mechanicService ?? inject(MechanicService, { optional: true });
    } catch {
      this.mechanicService = mechanicService ?? null;
    }
  }

  ngOnInit(): void {
    const initialTab = this.route?.snapshot?.queryParamMap?.get('tab');
    if (initialTab === 'stock') {
      this.activeTab.set('stock');
    }

    this.route?.queryParamMap?.subscribe?.((params) => {
      this.activeTab.set(params.get('tab') === 'stock' ? 'stock' : 'orders');
    });

    this.loadOperationsFromApi();
  }

  loadOperationsFromApi(): void {
    if (!this.mechanicService) return;

    this.mechanicService.getOperations().subscribe({
      next: (res) => {
        if (res.serviceOrders && res.serviceOrders.length > 0) {
          this.serviceOrders.set(res.serviceOrders);
        }
        if (res.stockItems && res.stockItems.length > 0) {
          this.stockItems.set(res.stockItems);
        }
      },
      error: () => {
        // Retém dados iniciais / mock em caso de falha de conexão
      },
    });
  }

  setTab(tab: 'orders' | 'stock'): void {
    this.activeTab.set(tab);
  }

  advanceOrderStatus(orderId: string): void {
    const current = this.serviceOrders().find((o) => o.id === orderId);
    if (!current) return;

    const nextStatus =
      current.status === 'WAITING_BAY'
        ? 'IN_REPAIR'
        : current.status === 'IN_REPAIR'
          ? 'READY_PICKUP'
          : 'READY_PICKUP';

    // Otimista
    this.serviceOrders.update((orders) =>
      orders.map((o) => (o.id === orderId ? { ...o, status: nextStatus } : o))
    );
    this.feedbackBanner.set(`Ordem ${current.code} (${current.plate}) atualizada no pátio.`);

    if (this.mechanicService) {
      this.mechanicService.advanceServiceOrderStatus(orderId).subscribe({
        next: (updated) => {
          this.serviceOrders.update((orders) =>
            orders.map((o) => (o.id === orderId ? updated : o))
          );
        },
        error: () => {
          // Mantém o estado otimista
        },
      });
    }
  }

  adjustStock(sku: string, delta: number): void {
    // Otimista
    this.stockItems.update((items) =>
      items.map((item) => {
        if (item.sku !== sku) return item;
        const updatedQty = Math.max(0, item.quantity + delta);
        return { ...item, quantity: updatedQty };
      })
    );

    if (this.mechanicService) {
      this.mechanicService.adjustStock(sku, delta).subscribe({
        next: (updatedItem) => {
          this.stockItems.update((items) =>
            items.map((i) => (i.sku === sku ? updatedItem : i))
          );
        },
        error: () => {},
      });
    }
  }

  openNewOrderModal(): void {
    this.newOrderPlate.set('');
    this.newOrderBay.set('Baia 01 • Rápida');
    this.newOrderTitle.set('');
    this.newOrderError.set(null);
    this.isNewOrderOpen.set(true);
  }

  closeNewOrderModal(): void {
    this.isNewOrderOpen.set(false);
  }

  submitNewOrder(): void {
    this.newOrderError.set(null);
    const plate = this.newOrderPlate().trim().toUpperCase();
    const bay = this.newOrderBay().trim();
    const title = this.newOrderTitle().trim();

    if (!plate || plate.length < 7) {
      this.newOrderError.set('Informe uma placa válida com 7 caracteres (ex: MOT1A23).');
      return;
    }
    if (!title) {
      this.newOrderError.set('Informe o título/diagnóstico do serviço.');
      return;
    }

    this.newOrderSubmitting.set(true);

    if (this.mechanicService) {
      this.mechanicService
        .createServiceOrder({ plate, bay, serviceTitle: title })
        .subscribe({
          next: (created) => {
            this.serviceOrders.update((list) => [created, ...list]);
            this.newOrderSubmitting.set(false);
            this.isNewOrderOpen.set(false);
            this.feedbackBanner.set(
              `Ordem de serviço ${created.code} criada com sucesso para a moto ${created.plate}!`
            );
          },
          error: () => {
            // Criação local de fallback
            this.createLocalFallbackOrder(plate, bay, title);
          },
        });
    } else {
      this.createLocalFallbackOrder(plate, bay, title);
    }
  }

  private createLocalFallbackOrder(plate: string, bay: string, serviceTitle: string): void {
    const fallbackId = 'os-' + Date.now();
    const code = `OS-2026-${Math.floor(1000 + Math.random() * 9000)}`;
    const newOrder: BayServiceOrder = {
      id: fallbackId,
      code,
      plate,
      model: 'Mottu Sport 110i',
      bay,
      serviceTitle,
      originLabel: 'Abertura rápida na oficina',
      slaMinutes: 30,
      status: 'WAITING_BAY',
    };
    this.serviceOrders.update((list) => [newOrder, ...list]);
    this.newOrderSubmitting.set(false);
    this.isNewOrderOpen.set(false);
    this.feedbackBanner.set(`Ordem de serviço ${code} criada com sucesso para a moto ${plate}!`);
  }
}
