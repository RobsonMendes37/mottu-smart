import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FleetService, FleetFilter } from '../../services/fleet.service';
import {
  CreateMotorcycleCommand,
  FleetMotorcycleResponse,
  MotorcycleModel,
  MotorcycleStatus,
  RecordBaseline360Command,
  YardResponse,
} from '../../models/mechanic.models';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';

export interface StatusBadgeInfo {
  label: string;
  colorClass: string;
  badgeBg: string;
  borderColor: string;
  dotColor: string;
}

@Component({
  selector: 'app-t07-fleet',
  standalone: true,
  imports: [CommonModule, FormsModule, DesktopShellComponent],
  templateUrl: './t07-fleet.component.html',
})
export class T07FleetComponent implements OnInit {
  private fleetService: FleetService;

  motorcycles = signal<FleetMotorcycleResponse[]>([]);
  yards = signal<YardResponse[]>([]);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  statusFilter = signal<MotorcycleStatus | ''>('');
  plateFilter = signal<string>('');
  yardFilter = signal<string>('');
  totalElements = signal<number>(0);
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);

  // Slide-over Drawer States (Create & Inspect)
  isModalOpen = signal<boolean>(false);
  isSubmittingModal = signal<boolean>(false);
  modalError = signal<string | null>(null);
  selectedMotorcycle = signal<FleetMotorcycleResponse | null>(null);

  availableCount = computed(
    () => this.motorcycles().filter((m) => m.status === 'AVAILABLE').length
  );
  rentedCount = computed(
    () => this.motorcycles().filter((m) => m.status === 'RENTED').length
  );
  maintenanceCount = computed(
    () => this.motorcycles().filter((m) => m.status === 'IN_MAINTENANCE').length
  );

  // Modal Form Signals
  newPlate = signal<string>('');
  newModel = signal<MotorcycleModel>('MOTTU_SPORT_110I');
  newYardId = signal<string>('');
  newOdometer = signal<number>(0);

  // Baseline 360 Modal Signals
  isBaselineModalOpen = signal<boolean>(false);
  baselineMotorcycle = signal<FleetMotorcycleResponse | null>(null);
  baselineFront = signal<string>('/demo-photos/vistoria-frente.webp');
  baselineRear = signal<string>('/demo-photos/vistoria-traseira.webp');
  baselineRight = signal<string>('/demo-photos/vistoria-lateral-direita.webp');
  baselineLeft = signal<string>('/demo-photos/vistoria-lateral-esquerda.webp');
  baselineOdometer = signal<string>('/demo-photos/vistoria-odometro.webp');
  baselineOdometerKm = signal<number>(0);
  isSubmittingBaseline = signal<boolean>(false);
  baselineError = signal<string | null>(null);

  readonly availableModels: { value: MotorcycleModel; label: string }[] = [
    { value: 'MOTTU_SPORT_110I', label: 'Mottu Sport 110i' },
    { value: 'HONDA_POP_110I', label: 'Honda Pop 110i' },
    { value: 'TVS_APACHE_160', label: 'TVS Apache 160' },
    { value: 'TVS_RAIDER_125', label: 'TVS Raider 125' },
  ];

  constructor(fleetService?: FleetService) {
    try {
      this.fleetService = fleetService ?? inject(FleetService);
    } catch {
      this.fleetService = fleetService as FleetService;
    }
  }

  ngOnInit(): void {
    this.loadYards();
    this.loadMotorcycles();
  }

  loadYards(): void {
    this.fleetService.getYards().subscribe({
      next: (data) => {
        this.yards.set(data);
        if (data.length > 0 && !this.newYardId()) {
          this.newYardId.set(data[0].id);
        }
      },
      error: () => {},
    });
  }

  loadMotorcycles(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const filter: FleetFilter = {};
    if (this.statusFilter()) filter.status = this.statusFilter() as MotorcycleStatus;
    if (this.plateFilter()) filter.plate = this.plateFilter();
    if (this.yardFilter()) filter.yardId = this.yardFilter();

    this.fleetService
      .listMotorcycles(filter, this.currentPage(), this.pageSize())
      .subscribe({
        next: (page) => {
          this.motorcycles.set(page.items);
          this.totalElements.set(page.totalElements);
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Não foi possível carregar a frota de motos.');
          this.isLoading.set(false);
        },
      });
  }

  setStatusFilter(status: MotorcycleStatus | ''): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadMotorcycles();
  }

  onPlateFilter(plate: string): void {
    this.plateFilter.set(plate.trim().toUpperCase());
    this.currentPage.set(0);
    this.loadMotorcycles();
  }

  onYardFilter(yardId: string): void {
    this.yardFilter.set(yardId);
    this.currentPage.set(0);
    this.loadMotorcycles();
  }

  quickToggleStatus(motorcycleId: string, newStatus: MotorcycleStatus): void {
    this.fleetService.updateStatus(motorcycleId, { status: newStatus }).subscribe({
      next: (res) => {
        this.successMessage.set(`Status atualizado com sucesso para moto ${res.plate}!`);
        setTimeout(() => this.successMessage.set(null), 4000);
        this.loadMotorcycles();
      },
      error: () => {
        this.errorMessage.set('Não foi possível alterar o status da motocicleta.');
        setTimeout(() => this.errorMessage.set(null), 4000);
      },
    });
  }

  openCreateModal(): void {
    this.newPlate.set('');
    this.newModel.set('MOTTU_SPORT_110I');
    if (this.yards().length > 0) {
      this.newYardId.set(this.yards()[0].id);
    }
    this.newOdometer.set(0);
    this.modalError.set(null);
    this.isModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isModalOpen.set(false);
    this.modalError.set(null);
  }

  submitCreateMotorcycle(): void {
    const plate = this.newPlate().trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (!plate || plate.length < 5) {
      this.modalError.set('Placa é obrigatória (mínimo 5 caracteres no formato Mercosul).');
      return;
    }

    if (!this.newYardId()) {
      this.modalError.set('Selecione um galpão para vincular a moto.');
      return;
    }

    if (this.newOdometer() < 0) {
      this.modalError.set('O hodômetro inicial não pode ser negativo.');
      return;
    }

    this.isSubmittingModal.set(true);
    this.modalError.set(null);

    const command: CreateMotorcycleCommand = {
      plate,
      model: this.newModel(),
      yardId: this.newYardId(),
      currentOdometerKm: Number(this.newOdometer()) || 0,
    };

    this.fleetService.registerMotorcycle(command).subscribe({
      next: (created) => {
        this.isSubmittingModal.set(false);
        this.closeCreateModal();
        this.successMessage.set(`Moto ${created.plate} cadastrada com sucesso!`);
        setTimeout(() => this.successMessage.set(null), 4000);
        this.loadMotorcycles();
      },
      error: (err) => {
        this.isSubmittingModal.set(false);
        this.modalError.set(
          err?.error?.detail || 'Erro ao cadastrar motocicleta. Verifique se a placa já existe.'
        );
      },
    });
  }

  openInspectDrawer(moto: FleetMotorcycleResponse): void {
    this.isModalOpen.set(false);
    this.isBaselineModalOpen.set(false);
    this.selectedMotorcycle.set(moto);
  }

  closeInspectDrawer(): void {
    this.selectedMotorcycle.set(null);
  }

  openBaselineModal(moto: FleetMotorcycleResponse): void {
    this.isModalOpen.set(false);
    this.selectedMotorcycle.set(null);
    this.baselineMotorcycle.set(moto);
    this.baselineOdometerKm.set(moto.currentOdometerKm || 0);
    this.baselineFront.set('/demo-photos/vistoria-frente.webp');
    this.baselineRear.set('/demo-photos/vistoria-traseira.webp');
    this.baselineRight.set('/demo-photos/vistoria-lateral-direita.webp');
    this.baselineLeft.set('/demo-photos/vistoria-lateral-esquerda.webp');
    this.baselineOdometer.set('/demo-photos/vistoria-odometro.webp');
    this.baselineError.set(null);
    this.isBaselineModalOpen.set(true);
  }

  closeBaselineModal(): void {
    this.isBaselineModalOpen.set(false);
    this.baselineMotorcycle.set(null);
    this.baselineError.set(null);
  }

  submitBaseline360(): void {
    const moto = this.baselineMotorcycle();
    if (!moto) return;

    this.isSubmittingBaseline.set(true);
    this.baselineError.set(null);

    const command: RecordBaseline360Command = {
      frontPhotoUrl: this.baselineFront().trim(),
      rearPhotoUrl: this.baselineRear().trim(),
      rightSidePhotoUrl: this.baselineRight().trim(),
      leftSidePhotoUrl: this.baselineLeft().trim(),
      odometerPhotoUrl: this.baselineOdometer().trim(),
      initialOdometerKm: Number(this.baselineOdometerKm()) || 0,
    };

    this.fleetService.recordBaseline360(moto.id, command).subscribe({
      next: () => {
        this.isSubmittingBaseline.set(false);
        this.closeBaselineModal();
        this.successMessage.set(`Giro 360 de Expedição gravado com sucesso para moto ${moto.plate}!`);
        setTimeout(() => this.successMessage.set(null), 4000);
        this.loadMotorcycles();
      },
      error: (err) => {
        this.isSubmittingBaseline.set(false);
        this.baselineError.set(
          err?.error?.detail || 'Erro ao registrar fotos da vistoria 360.'
        );
      },
    });
  }

  getStatusBadge(status: MotorcycleStatus): StatusBadgeInfo {
    switch (status) {
      case 'AVAILABLE':
        return {
          label: 'Disponível no pátio',
          colorClass: 'text-[#046C3A]',
          badgeBg: 'bg-[#E6F4EE]',
          borderColor: 'border-[#05944F]/30',
          dotColor: 'bg-[#05944F]',
        };
      case 'RENTED':
        return {
          label: 'Alugada em rota',
          colorClass: 'text-[#141414]',
          badgeBg: 'bg-[#F3F3F3]',
          borderColor: 'border-[#E2E2E2]',
          dotColor: 'bg-[#141414]',
        };
      case 'IN_MAINTENANCE':
        return {
          label: 'Na oficina',
          colorClass: 'text-[#92400E]',
          badgeBg: 'bg-[#FEF3C7]',
          borderColor: 'border-[#D97706]/30',
          dotColor: 'bg-[#D97706]',
        };
      case 'UNAVAILABLE':
      default:
        return {
          label: 'Indisponível',
          colorClass: 'text-[#B91C1C]',
          badgeBg: 'bg-[#FEE2E2]',
          borderColor: 'border-[#DC2626]/30',
          dotColor: 'bg-[#DC2626]',
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
}
