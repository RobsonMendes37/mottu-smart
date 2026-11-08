import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';

export interface BayServiceOrder {
  id: string;
  code: string;
  plate: string;
  model: string;
  bay: string;
  serviceTitle: string;
  originLabel: string;
  slaMinutes: number;
  status: 'WAITING_BAY' | 'IN_REPAIR' | 'READY_PICKUP';
}

export interface LocalStockItem {
  sku: string;
  partName: string;
  model: string;
  aisleBin: string;
  quantity: number;
  minQuantity: number;
  unitPriceBrl: number;
}

@Component({
  selector: 'app-mechanic-operations',
  standalone: true,
  imports: [CommonModule, FormsModule, DesktopShellComponent],
  templateUrl: './mechanic-operations.component.html',
})
export class MechanicOperationsComponent implements OnInit {
  private route: ActivatedRoute | null;

  activeTab = signal<'orders' | 'stock'>('orders');
  stockSearch = signal<string>('');
  feedbackBanner = signal<string | null>(null);

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

  constructor(route?: ActivatedRoute) {
    try {
      this.route = route ?? inject(ActivatedRoute, { optional: true });
    } catch {
      this.route = route ?? null;
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
  }

  setTab(tab: 'orders' | 'stock'): void {
    this.activeTab.set(tab);
  }

  advanceOrderStatus(orderId: string): void {
    this.serviceOrders.update((orders) =>
      orders.map((order) => {
        if (order.id !== orderId) return order;
        const nextStatus =
          order.status === 'WAITING_BAY'
            ? 'IN_REPAIR'
            : order.status === 'IN_REPAIR'
              ? 'READY_PICKUP'
              : 'READY_PICKUP';
        this.feedbackBanner.set(`Ordem ${order.code} (${order.plate}) atualizada no pátio.`);
        return { ...order, status: nextStatus };
      })
    );
  }

  adjustStock(sku: string, delta: number): void {
    this.stockItems.update((items) =>
      items.map((item) => {
        if (item.sku !== sku) return item;
        const updatedQty = Math.max(0, item.quantity + delta);
        return { ...item, quantity: updatedQty };
      })
    );
  }
}

