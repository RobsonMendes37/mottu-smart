import { describe, it, expect, beforeEach, vi } from 'vitest';
import { MechanicOperationsComponent } from './mechanic-operations.component';

describe('MechanicOperationsComponent (Screen T07-Mobile & Web - Ordens de serviço e estoque)', () => {
  let comp: MechanicOperationsComponent;
  let mockRoute: any;

  beforeEach(() => {
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

    comp = new MechanicOperationsComponent(mockRoute);
    comp.ngOnInit();
  });

  it('TC-OPS-1: Exposes bay service orders and local warehouse parts', () => {
    expect(comp.serviceOrders().length).toBeGreaterThan(0);
    expect(comp.stockItems().length).toBeGreaterThan(0);
  });

  it('TC-OPS-2: Advances service order status on bay action', () => {
    const firstOrder = comp.serviceOrders()[0];
    comp.advanceOrderStatus(firstOrder.id);
    const updated = comp.serviceOrders().find((o) => o.id === firstOrder.id);
    expect(updated?.status).not.toBe(firstOrder.status);
  });

  it('TC-OPS-3: Filters warehouse stock by SKU/name and adjusts bin quantity', () => {
    const target = comp.stockItems()[0];
    const initialQty = target.quantity;

    comp.adjustStock(target.sku, 2);
    const afterAdd = comp.stockItems().find((i) => i.sku === target.sku);
    expect(afterAdd?.quantity).toBe(initialQty + 2);

    comp.stockSearch.set(target.sku);
    expect(comp.filteredStockItems().length).toBe(1);
  });

  it('TC-OPS-4: Opens new order modal and creates service order', () => {
    comp.openNewOrderModal();
    expect(comp.isNewOrderOpen()).toBe(true);

    comp.newOrderPlate.set('MOT9Z99');
    comp.newOrderBay.set('Baia 02');
    comp.newOrderTitle.set('Troca de retrovisor');

    comp.submitNewOrder();
    expect(comp.isNewOrderOpen()).toBe(false);
    expect(comp.serviceOrders().some((o) => o.plate === 'MOT9Z99')).toBe(true);
    expect(comp.feedbackBanner()).toContain('criada com sucesso');
  });

  it('TC-OPS-5: Validates plate format before submitting new service order', () => {
    comp.openNewOrderModal();
    comp.newOrderPlate.set('123'); // Invalid short plate
    comp.newOrderTitle.set('Teste');
    comp.submitNewOrder();

    expect(comp.isNewOrderOpen()).toBe(true);
    expect(comp.newOrderError()).toContain('7 caracteres');
  });
});

