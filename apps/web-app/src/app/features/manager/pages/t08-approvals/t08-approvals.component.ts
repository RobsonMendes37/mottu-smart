import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ManagerService } from '../../services/manager.service';
import {
  PendingUserResponseDto,
  AvailableMotorcycleOption,
} from '../../models/manager.models';

@Component({
  selector: 'app-t08-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './t08-approvals.component.html',
})
export class T08ApprovalsComponent implements OnInit {
  private managerService: ManagerService;

  pendingUsers = signal<PendingUserResponseDto[]>([]);
  availableMotorcycles = signal<AvailableMotorcycleOption[]>([]);
  selectedBikes = signal<Record<string, string>>({});

  isLoading = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Reject Modal state
  rejectingUser = signal<PendingUserResponseDto | null>(null);
  rejectReason = signal<string>('');
  rejectValidationError = signal<string | null>(null);

  // Filter state ('ALL' | 'RIDER' | 'MECHANIC')
  activeFilter = signal<'ALL' | 'RIDER' | 'MECHANIC'>('ALL');

  filteredUsers = computed(() => {
    const filter = this.activeFilter();
    const users = this.pendingUsers();
    if (filter === 'ALL') return users;
    return users.filter((u) => u.role === filter);
  });

  riderCount = computed(() => this.pendingUsers().filter((u) => u.role === 'RIDER').length);
  mechanicCount = computed(() => this.pendingUsers().filter((u) => u.role === 'MECHANIC').length);

  constructor(managerService?: ManagerService) {
    try {
      this.managerService = managerService ?? inject(ManagerService);
    } catch {
      this.managerService = managerService as ManagerService;
    }
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.managerService.getPendingUsers().subscribe({
      next: (users) => {
        this.pendingUsers.set(users);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Erro ao carregar lista de usuários pendentes.');
        this.isLoading.set(false);
      },
    });

    this.managerService.getAvailableMotorcycles().subscribe({
      next: (bikes) => {
        this.availableMotorcycles.set(bikes);
      },
      error: () => {
        // Non-blocking if fleet is momentarily unavailable
      },
    });
  }

  selectMotorcycle(userId: string, motorcycleId: string): void {
    this.selectedBikes.update((map) => ({ ...map, [userId]: motorcycleId }));
    this.errorMessage.set(null);
  }

  approve(user: PendingUserResponseDto): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (user.role === 'RIDER') {
      const motoId = this.selectedBikes()[user.id];
      if (!motoId) {
        this.errorMessage.set(
          `Selecione uma moto disponível no galpão para aprovar o entregador ${user.fullName}.`
        );
        return;
      }

      this.isSubmitting.set(true);
      this.managerService.approveUser(user.id, { motorcycleId: motoId }).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.successMessage.set(`Entregador ${user.fullName} aprovado com sucesso! Contrato de locação iniciado.`);
          this.removeUserFromList(user.id);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const detail = err?.error?.detail || err?.message || 'Falha ao aprovar cadastro';
          this.errorMessage.set(`Erro ao aprovar ${user.fullName}: ${detail}`);
        },
      });
    } else {
      // MECHANIC does not require motorcycle
      this.isSubmitting.set(true);
      this.managerService.approveUser(user.id).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.successMessage.set(`Mecânico ${user.fullName} aprovado com sucesso! Acesso liberado.`);
          this.removeUserFromList(user.id);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const detail = err?.error?.detail || err?.message || 'Falha ao aprovar cadastro';
          this.errorMessage.set(`Erro ao aprovar ${user.fullName}: ${detail}`);
        },
      });
    }
  }

  openRejectModal(user: PendingUserResponseDto): void {
    this.rejectingUser.set(user);
    this.rejectReason.set('');
    this.rejectValidationError.set(null);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  closeRejectModal(): void {
    this.rejectingUser.set(null);
    this.rejectReason.set('');
    this.rejectValidationError.set(null);
  }

  setRejectReason(reason: string): void {
    this.rejectReason.set(reason);
    if (reason.trim().length >= 5) {
      this.rejectValidationError.set(null);
    }
  }

  confirmReject(): void {
    const user = this.rejectingUser();
    if (!user) return;

    const reason = this.rejectReason().trim();
    if (reason.length < 5) {
      this.rejectValidationError.set('O motivo da recusa deve ter pelo menos 5 caracteres.');
      return;
    }

    this.isSubmitting.set(true);
    this.managerService.rejectUser(user.id, { reason }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set(`Cadastro de ${user.fullName} recusado com sucesso.`);
        this.removeUserFromList(user.id);
        this.closeRejectModal();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const detail = err?.error?.detail || err?.message || 'Falha ao recusar cadastro';
        this.errorMessage.set(`Erro ao recusar ${user.fullName}: ${detail}`);
      },
    });
  }

  setFilter(filter: 'ALL' | 'RIDER' | 'MECHANIC'): void {
    this.activeFilter.set(filter);
  }

  private removeUserFromList(userId: string): void {
    this.pendingUsers.update((users) => users.filter((u) => u.id !== userId));
  }
}
