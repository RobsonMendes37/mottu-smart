import { describe, it, expect } from 'vitest';
import { routes } from './app.routes';
import { Route } from '@angular/router';

describe('AppRoutes Configuration', () => {
  it('TC-ROUTE-1: Defines all top-level application routes', () => {
    const paths = routes.map((r) => r.path);
    expect(paths).toContain('');
    expect(paths).toContain('login');
    expect(paths).toContain('rider');
    expect(paths).toContain('mechanic');
    expect(paths).toContain('manager');
    expect(paths).toContain('**');
  });

  it('TC-ROUTE-2: Root path redirects to login', () => {
    const rootRoute = routes.find((r) => r.path === '');
    expect(rootRoute).toBeDefined();
    expect(rootRoute?.redirectTo).toBe('login');
    expect(rootRoute?.pathMatch).toBe('full');
  });

  it('TC-ROUTE-3: Wildcard route redirects to login', () => {
    const wildcardRoute = routes.find((r) => r.path === '**');
    expect(wildcardRoute).toBeDefined();
    expect(wildcardRoute?.redirectTo).toBe('login');
  });

  describe('Rider Routes & Accident Redirection', () => {
    const riderRoute = routes.find((r) => r.path === 'rider') as Route;

    it('TC-ROUTE-RIDER-1: Rider route is configured with guards and children', () => {
      expect(riderRoute).toBeDefined();
      expect(riderRoute.canActivate).toBeDefined();
      expect(riderRoute.children).toBeDefined();
    });

    it('TC-ROUTE-RIDER-2: Rider accident routes load wizard and quote components', async () => {
      const children = riderRoute.children || [];
      const wizardRoute = children.find((c) => c.path === 'accidents/new');
      const quoteRoute = children.find((c) => c.path === 'accidents/:id');

      expect(wizardRoute).toBeDefined();
      expect(typeof wizardRoute?.loadComponent).toBe('function');
      const wizardComp = await (wizardRoute!.loadComponent as any)();
      expect(wizardComp.name).toBe('AccidentWizardComponent');

      expect(quoteRoute).toBeDefined();
      expect(typeof quoteRoute?.loadComponent).toBe('function');
      const quoteComp = await (quoteRoute!.loadComponent as any)();
      expect(quoteComp.name).toBe('AccidentQuoteComponent');
    });
  });

  describe('Mechanic Routes & Accident Dossier', () => {
    const mechanicRoute = routes.find((r) => r.path === 'mechanic') as Route;

    it('TC-ROUTE-MECH-1: Mechanic route is configured with guards and children', () => {
      expect(mechanicRoute).toBeDefined();
      expect(mechanicRoute.canActivate).toBeDefined();
      expect(mechanicRoute.children).toBeDefined();
    });

    it('TC-ROUTE-MECH-2: Mechanic route loads T13AccidentDossierComponent for accidents/:id', async () => {
      const children = mechanicRoute.children || [];
      const dossierRoute = children.find((c) => c.path === 'accidents/:id');

      expect(dossierRoute).toBeDefined();
      expect(typeof dossierRoute?.loadComponent).toBe('function');
      const dossierComp = await (dossierRoute!.loadComponent as any)();
      expect(dossierComp.name).toBe('T13AccidentDossierComponent');
    });
  });

  describe('Manager Routes & Dashboard Redirection', () => {
    const managerRoute = routes.find((r) => r.path === 'manager') as Route;

    it('TC-ROUTE-4: Manager route is configured with canActivate guards', () => {
      expect(managerRoute).toBeDefined();
      expect(managerRoute.canActivate).toBeDefined();
      expect(managerRoute.canActivate?.length).toBe(2);
      expect(managerRoute.children).toBeDefined();
    });

    it('TC-ROUTE-5: Manager route defines /manager/dashboard redirecting to approvals', () => {
      const children = managerRoute.children || [];
      const dashboardRoute = children.find((c) => c.path === 'dashboard');

      expect(dashboardRoute).toBeDefined();
      expect(dashboardRoute?.redirectTo).toBe('approvals');
      expect(dashboardRoute?.pathMatch).toBe('full');
    });

    it('TC-ROUTE-6: Manager default route redirects to approvals', () => {
      const children = managerRoute.children || [];
      const defaultRoute = children.find((c) => c.path === '');

      expect(defaultRoute).toBeDefined();
      expect(defaultRoute?.redirectTo).toBe('approvals');
      expect(defaultRoute?.pathMatch).toBe('full');
    });

    it('TC-ROUTE-7: Manager child routes load approvals, catalog, and analytics components', async () => {
      const children = managerRoute.children || [];
      const approvalsRoute = children.find((c) => c.path === 'approvals');
      const catalogRoute = children.find((c) => c.path === 'catalog');
      const analyticsRoute = children.find((c) => c.path === 'analytics');

      expect(approvalsRoute).toBeDefined();
      expect(typeof approvalsRoute?.loadComponent).toBe('function');
      const approvalsComp = await (approvalsRoute!.loadComponent as any)();
      expect(approvalsComp.name).toBe('T08ApprovalsComponent');

      expect(catalogRoute).toBeDefined();
      expect(typeof catalogRoute?.loadComponent).toBe('function');
      const catalogComp = await (catalogRoute!.loadComponent as any)();
      expect(catalogComp.name).toBe('T09CatalogComponent');

      expect(analyticsRoute).toBeDefined();
      expect(typeof analyticsRoute?.loadComponent).toBe('function');
      const analyticsComp = await (analyticsRoute!.loadComponent as any)();
      expect(analyticsComp.name).toBe('T10AnalyticsComponent');
    });
  });
});
