import { describe, it, expect, beforeEach, vi } from 'vitest';
import { GarageComponent } from './garage.component';
import { RiderStore } from '../../state/rider.store';
import { AuthService } from '../../../../core/services/auth.service';
import { Router } from '@angular/router';

describe('GarageComponent (Screen T02 - Minha Moto / Garagem)', () => {
  let comp: GarageComponent;
  let mockStore: any;
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(() => {
    const activeBike = {
      id: 'moto-1',
      plate: 'MOT1A23',
      model: 'MOTTU_SPORT_110I',
      status: 'RENTED',
      currentOdometerKm: 1035,
      yardName: 'Butantã',
    };

    mockStore = {
      garage: () => null,
      isLoadingGarage: () => false,
      garageError: () => null,
      activeMotorcycle: () => activeBike,
      openInspectionsCount: () => 1,
      hasReachedQuota: () => false,
      loadGarage: vi.fn().mockResolvedValue(undefined),
    };

    mockAuthService = {
      currentUser: () => ({ fullName: 'Carlos Silva', role: 'RIDER' }),
      logout: vi.fn(),
    };

    mockRouter = {
      navigate: vi.fn(),
    };

    comp = new GarageComponent(mockStore as any, mockAuthService as any, mockRouter as any);
  });

  it('TC-T02-1: Calls loadGarage on init and extracts rider first name', () => {
    comp.ngOnInit();
    expect(mockStore.loadGarage).toHaveBeenCalled();
    expect(comp.riderGreeting()).toBe('Olá, Carlos');
  });

  it('TC-T02-2: Displays active bike telemetry correctly', () => {
    expect(comp.motorcycle()?.plate).toBe('MOT1A23');
    expect(comp.motorcycle()?.currentOdometerKm).toBe(1035);
    expect(comp.motorcycle()?.yardName).toBe('Butantã');
  });

  it('TC-T02-3: Navigates to new inspection wizard when quota is not reached', () => {
    comp.onNewInspection();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/rider/inspections/new']);
  });

  it('TC-T02-4: Blocks navigation and shows warning if quota (3 open) is reached', () => {
    (mockStore.hasReachedQuota as any) = () => true;
    (mockStore.openInspectionsCount as any) = () => 3;

    comp.onNewInspection();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(comp.quotaWarningVisible()).toBe(true);
  });

  it('TC-T02-5: Navigates to inspection status when recent ticket clicked', () => {
    comp.onViewInspection('ins-uuid-123');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/rider/inspections', 'ins-uuid-123']);
  });

  it('TC-T02-6: Navigates to accident reporting wizard on CTA click', () => {
    comp.onReportAccident();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/rider/accidents/new']);
  });
});
