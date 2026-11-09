import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { RiderRentalComponent } from './rider-rental.component';

describe('RiderRentalComponent (Screen T04C - Aluguel semanal e entregas)', () => {
  let comp: RiderRentalComponent;
  let mockRiderStore: any;
  let mockRoute: any;

  beforeEach(() => {
    mockRiderStore = {
      garage: signal({
        activeContract: {
          contractId: 'ctr-1',
          pickedUpAt: '2026-11-01T08:00:00Z',
          initialOdometerKm: 1000,
          motorcycle: {
            id: 'm-1',
            plate: 'MOT1A23',
            model: 'MOTTU_SPORT_110I',
            status: 'RENTED',
            currentOdometerKm: 1240,
            yardName: 'Butantã',
          },
        },
        openInspectionsCount: 0,
        recentInspections: [
          {
            id: 'ins-1',
            status: 'AUTO_RESOLVED',
            finalVerdict: 'MAU_USO',
            totalEstimatedBrl: 60.0,
            createdAt: '2026-11-06T10:00:00Z',
          },
        ],
      }),
      activeMotorcycle: signal({
        id: 'm-1',
        plate: 'MOT1A23',
        model: 'MOTTU_SPORT_110I',
        status: 'RENTED',
        currentOdometerKm: 1240,
        yardName: 'Butantã',
      }),
      loadGarage: vi.fn().mockResolvedValue(undefined),
    };

    mockRoute = {
      snapshot: {
        queryParamMap: {
          get: vi.fn().mockReturnValue(null),
        },
      },
      queryParamMap: {
        subscribe: vi.fn(),
      },
    };

    comp = new RiderRentalComponent(mockRiderStore, mockRoute);
    comp.ngOnInit();
  });

  it('TC-T04C-1: Computes weekly total combining base rental, punctuality discount and MAU_USO inspection items', () => {
    // Base 196.00 - 15.00 punctuality + 60.00 inspection = 241.00
    expect(comp.baseWeeklyPlanBrl).toBe(196.0);
    expect(comp.punctualityDiscountBrl).toBe(15.0);
    expect(comp.inspectionChargesBrl()).toBe(60.0);
    expect(comp.weeklyTotalBrl()).toBe(241.0);
  });

  it('TC-T04C-2: Copies Pix code and sets visual confirmation flag', async () => {
    await comp.copyPixCode();
    expect(comp.pixCopied()).toBe(true);
  });

  it('TC-T04C-3: Switches between rental and deliveries sections cleanly', () => {
    expect(comp.activeSection()).toBe('rental');
    comp.setSection('deliveries');
    expect(comp.activeSection()).toBe('deliveries');
    expect(comp.shellActiveTab()).toBe('deliveries');
  });

  it('TC-T04C-4: payPix updates payment status to PAID and sets confirmation message', () => {
    expect(comp.paymentStatus()).toBe('PENDING_PIX');
    comp.payPix();
    expect(comp.paymentStatus()).toBe('PAID');
    expect(comp.feedbackMessage()).toContain('confirmado com sucesso');
  });

  it('TC-T04C-5: submitDeliveries adds daily count and unlocks R$ 40,00 bonus when reaching 120', () => {
    comp.setSection('deliveries');
    const initialCount = comp.weeklyDeliveriesTotal();
    expect(initialCount).toBe(118);

    comp.toggleRegisterDeliveries();
    expect(comp.isRegisterDeliveriesOpen()).toBe(true);

    comp.newDeliveriesCount.set(5);
    comp.newDeliveriesEarnings.set(50.0);
    comp.submitDeliveries();

    expect(comp.isRegisterDeliveriesOpen()).toBe(false);
    expect(comp.weeklyDeliveriesTotal()).toBe(123);
    expect(comp.deliveryBonusBrl()).toBe(40.0);
    expect(comp.isGoalAchieved()).toBe(true);
    expect(comp.feedbackMessage()).toContain('bônus creditados');
  });
});

