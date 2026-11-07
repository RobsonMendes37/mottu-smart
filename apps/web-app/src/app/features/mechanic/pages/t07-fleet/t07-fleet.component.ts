import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FleetService, FleetFilter } from '../../services/fleet.service';
import {
  CreateMotorcycleCommand,
  FleetMotorcycleResponse,
  MotorcycleModel,
  MotorcycleStatus,
  YardResponse,
} from '../../models/mechanic.models';

export interface StatusBadgeInfo {
  label: string;
  colorClass: string;
  badgeBg: string;
  borderColor: string;
}

@Component({
  selector: 'app-t07-fleet',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
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

  // Modal State
  isModalOpen = signal<boolean>(false);
  isSubmittingModal = signal<boolean>(false);
  modalError = signal<string | null>(null);

  // Modal Form Signals
  newPlate = signal<string>('');
  newModel = signal<MotorcycleModel>('MOTTU_SPORT_110I');
  newYardId = signal<string>('');
  newOdometer = signal<number>(0);

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

  getStatusBadge(status: MotorcycleStatus): StatusBadgeInfo {
    switch (status) {
      case 'AVAILABLE':
        return {
          label: 'DISPONÍVEL NO PÁTIO',
          colorClass: 'text-[#00E676]',
          badgeBg: 'bg-[#00E676]/15',
          borderColor: 'border-[#00E676]/40',
        };
      case 'RENTED':
        return {
          label: 'ALUGADA',
          colorClass: 'text-[#F2F7F5]',
          badgeBg: 'bg-[#1A2320]',
          borderColor: 'border-[#26332E]',
        };
      case 'IN_MAINTENANCE':
        return {
          label: 'NA OFICINA',
          colorClass: 'text-[#FFB300]',
          badgeBg: 'bg-[#FFB300]/15',
          borderColor: 'border-[#FFB300]/40',
        };
      case 'UNAVAILABLE':
      default:
        return {
          label: 'INDISPONÍVEL',
          colorClass: 'text-[#FF4D4F]',
          badgeBg: 'bg-[#FF4D4F]/15',
          borderColor: 'border-[#FF4D4F]/40',
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
