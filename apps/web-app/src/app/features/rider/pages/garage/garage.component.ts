import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RiderStore } from '../../state/rider.store';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-rider-garage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './garage.component.html',
})
export class GarageComponent implements OnInit {
  protected store: RiderStore;
  protected authService: AuthService | null;
  protected router: Router | null;

  quotaWarningVisible = signal<boolean>(false);

  motorcycle = computed(() => {
    const storeActive = this.store.activeMotorcycle;
    return typeof storeActive === 'function' ? storeActive() : null;
  });

  openCount = computed(() => {
    const count = this.store.openInspectionsCount;
    return typeof count === 'function' ? count() : 0;
  });

  hasReachedQuota = computed(() => {
    const quota = this.store.hasReachedQuota;
    return typeof quota === 'function' ? quota() : false;
  });

  riderGreeting = computed(() => {
    const user = this.authService?.currentUser();
    if (!user || !user.fullName) return 'Olá, Entregador';
    const firstName = user.fullName.split(' ')[0];
    return `Olá, ${firstName}`;
  });

  constructor(
    store?: RiderStore,
    authService?: AuthService,
    router?: Router
  ) {
    try {
      this.store = store ?? inject(RiderStore);
    } catch {
      this.store = store as RiderStore;
    }
    try {
      this.authService = authService ?? inject(AuthService, { optional: true });
    } catch {
      this.authService = authService ?? null;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
  }

  ngOnInit(): void {
    this.store.loadGarage();
  }

  onNewInspection(): void {
    if (this.hasReachedQuota()) {
      this.quotaWarningVisible.set(true);
      return;
    }
    this.router?.navigate(['/rider/inspections/new']);
  }

  onReportAccident(): void {
    this.router?.navigate(['/rider/accidents/new']);
  }

  onViewInspection(inspectionId: string): void {
    this.router?.navigate(['/rider/inspections', inspectionId]);
  }

  onLogout(): void {
    this.authService?.logout();
  }
}
