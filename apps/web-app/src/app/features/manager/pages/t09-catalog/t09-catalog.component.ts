import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ManagerService } from '../../services/manager.service';
import {
  CatalogPartResponseDto,
  MotorcycleModel,
  SyncCatalogResponseDto,
} from '../../models/manager.models';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';

@Component({
  selector: 'app-t09-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, DesktopShellComponent],
  templateUrl: './t09-catalog.component.html',
})
export class T09CatalogComponent implements OnInit {
  private managerService: ManagerService;

  parts = signal<CatalogPartResponseDto[]>([]);
  isLoading = signal<boolean>(false);
  isSyncing = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Sync telemetry
  syncResult = signal<SyncCatalogResponseDto | null>(null);

  // Filters & Pagination
  selectedModel = signal<MotorcycleModel | ''>('');
  searchTerm = signal<string>('');
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // Standard Labor Fee
  standardLaborFee = signal<number>(35.0);
  laborFeeInput = signal<number>(35.0);
  isEditingLaborFee = signal<boolean>(false);

  // Slide-over Drawer Part Edit
  editingPartId = signal<string | null>(null);
  editingPrice = signal<number>(0);
  editingNaturalWear = signal<boolean>(false);

  editingPart = computed<CatalogPartResponseDto | null>(() => {
    const id = this.editingPartId();
    if (!id) return null;
    return this.parts().find((p) => p.id === id) ?? null;
  });

  naturalWearCount = computed(() => this.parts().filter((p) => p.isNaturalWear).length);

  readonly availableModels: { key: MotorcycleModel | ''; label: string }[] = [
    { key: '', label: 'Todas as Motos' },
    { key: 'MOTTU_SPORT_110I', label: 'Mottu Sport 110i' },
    { key: 'HONDA_POP_110I', label: 'Honda Pop 110i' },
    { key: 'TVS_APACHE_160', label: 'TVS Apache 160' },
    { key: 'TVS_RAIDER_125', label: 'TVS Raider 125' },
  ];

  constructor(managerService?: ManagerService) {
    try {
      this.managerService = managerService ?? inject(ManagerService);
    } catch {
      this.managerService = managerService as ManagerService;
    }
  }

  ngOnInit(): void {
    this.loadParts();
    this.loadSettings();
  }

  loadParts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const model = this.selectedModel() || undefined;
    const search = this.searchTerm();

    this.managerService
      .getParts(model, search, this.currentPage(), this.pageSize())
      .subscribe({
        next: (page) => {
          this.parts.set(page.items);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Não foi possível carregar as peças do catálogo.');
          this.isLoading.set(false);
        },
      });
  }

  loadSettings(): void {
    this.managerService.getSettings().subscribe({
      next: (settings) => {
        const fee = Number(settings.standardLaborFeeBrl) || 35.0;
        this.standardLaborFee.set(fee);
        this.laborFeeInput.set(fee);
      },
      error: () => {
        // Fallback default
        this.standardLaborFee.set(35.0);
        this.laborFeeInput.set(35.0);
      },
    });
  }

  setModelFilter(model: MotorcycleModel | ''): void {
    this.selectedModel.set(model);
    this.currentPage.set(0);
    this.loadParts();
  }

  onSearch(term: string): void {
    this.searchTerm.set(term);
    this.currentPage.set(0);
    this.loadParts();
  }

  syncMottuStore(): void {
    this.isSyncing.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.managerService.syncMottuStore().subscribe({
      next: (res) => {
        this.isSyncing.set(false);
        this.syncResult.set(res);

        if (res.usedFallback) {
          this.successMessage.set(
            `Modo offline/snapshot utilizado: ${res.syncedCount} peças atualizadas. Detalhe: ${res.message}`
          );
        } else {
          this.successMessage.set(
            `Sucesso! ${res.syncedCount} peças sincronizadas diretamente de loja.mottu.com.br.`
          );
        }
        this.loadParts();
      },
      error: (err) => {
        this.isSyncing.set(false);
        const detail = err?.error?.detail || err?.message || 'Falha na conexão com VTEX';
        this.errorMessage.set(`Erro ao sincronizar preços com loja da Mottu: ${detail}`);
      },
    });
  }

  startEditPart(part: CatalogPartResponseDto): void {
    this.editingPartId.set(part.id);
    this.editingPrice.set(Number(part.unitPriceBrl));
    this.editingNaturalWear.set(part.isNaturalWear);
    this.errorMessage.set(null);
  }

  cancelEdit(): void {
    this.editingPartId.set(null);
  }

  saveEditPart(): void {
    const partId = this.editingPartId();
    if (!partId) return;

    const price = Number(this.editingPrice());
    if (isNaN(price) || price < 0) {
      this.errorMessage.set('O preço deve ser maior ou igual a zero.');
      return;
    }

    const naturalWear = this.editingNaturalWear();

    this.managerService
      .updatePart(partId, { unitPriceBrl: price, isNaturalWear: naturalWear })
      .subscribe({
        next: (updated) => {
          this.parts.update((list) =>
            list.map((p) => (p.id === partId ? updated : p))
          );
          this.editingPartId.set(null);
          this.successMessage.set(
            `Peça "${updated.partName}" atualizada com sucesso! Preço manual preservado contra sobrescrita.`
          );
        },
        error: (err) => {
          const detail = err?.error?.detail || err?.message || 'Falha ao salvar peça';
          this.errorMessage.set(`Erro ao atualizar peça: ${detail}`);
        },
      });
  }

  setLaborFeeInput(fee: number): void {
    this.laborFeeInput.set(fee);
  }

  toggleEditLaborFee(): void {
    this.isEditingLaborFee.update((v) => !v);
  }

  saveLaborFee(): void {
    const fee = Number(this.laborFeeInput());
    if (isNaN(fee) || fee < 0) {
      this.errorMessage.set('A taxa de mão de obra deve ser um valor maior ou igual a zero.');
      return;
    }

    this.managerService.updateSettings({ standardLaborFeeBrl: fee }).subscribe({
      next: (res) => {
        const updated = Number(res.standardLaborFeeBrl);
        this.standardLaborFee.set(updated);
        this.laborFeeInput.set(updated);
        this.isEditingLaborFee.set(false);
        this.successMessage.set(
          `Taxa padrão de mão de obra atualizada para R$ ${updated.toFixed(2).replace('.', ',')}.`
        );
      },
      error: () => {
        this.errorMessage.set('Falha ao atualizar taxa padrão de mão de obra.');
      },
    });
  }

  goToPage(p: number): void {
    if (p >= 0 && p < this.totalPages()) {
      this.currentPage.set(p);
      this.loadParts();
    }
  }

  formatModelName(model: MotorcycleModel): string {
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
        return model;
    }
  }
}
