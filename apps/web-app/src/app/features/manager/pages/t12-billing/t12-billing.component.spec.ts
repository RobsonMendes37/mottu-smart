import { describe, it, expect, beforeEach } from 'vitest';
import { T12BillingComponent } from './t12-billing.component';

describe('T12BillingComponent (Screen T12 Web - Contratos e faturamento semanal)', () => {
  let comp: T12BillingComponent;

  beforeEach(() => {
    comp = new T12BillingComponent();
  });

  it('TC-BILL-1: Computes contract KPIs and lists weekly contracts', () => {
    expect(comp.contracts().length).toBeGreaterThanOrEqual(4);
    expect(comp.totalWeeklyRevenueBrl()).toBeGreaterThan(0);
  });

  it('TC-BILL-2: Filters contracts by payment status and search query', () => {
    comp.setFilter('OVERDUE');
    expect(comp.filteredContracts().every((c) => c.paymentStatus === 'OVERDUE')).toBe(true);

    comp.setFilter('ALL');
    comp.onSearch('MOT1A23');
    expect(comp.filteredContracts().some((c) => c.plate === 'MOT1A23')).toBe(true);
  });

  it('TC-BILL-3: Opens and closes right Slide-over Drawer with selected contract', () => {
    const target = comp.contracts()[0];
    comp.openDrawer(target);
    expect(comp.selectedContract()?.contractCode).toBe(target.contractCode);

    comp.closeDrawer();
    expect(comp.selectedContract()).toBeNull();
  });

  it('TC-BILL-4: Confirms Pix payment for pending contract', () => {
    const pending = comp.contracts().find((c) => c.paymentStatus === 'PENDING_PIX')!;
    expect(pending).toBeDefined();

    comp.openDrawer(pending);
    comp.confirmPix(pending.contractCode);

    const updated = comp.contracts().find((c) => c.contractCode === pending.contractCode);
    expect(updated?.paymentStatus).toBe('PAID');
    expect(comp.selectedContract()?.paymentStatus).toBe('PAID');
    expect(comp.feedbackBanner()).toContain('confirmado com sucesso');
  });
});

