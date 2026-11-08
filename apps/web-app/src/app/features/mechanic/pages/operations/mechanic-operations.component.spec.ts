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
});

