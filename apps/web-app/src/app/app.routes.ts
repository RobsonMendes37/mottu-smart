import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  // Protected Routes with Guards
  {
    path: 'rider',
    canActivate: [authGuard, roleGuard(['RIDER'])],
    children: [
      {
        path: '',
        redirectTo: 'garage',
        pathMatch: 'full',
      },
      {
        path: 'garage',
        loadComponent: () =>
          import('./features/rider/pages/garage/garage.component').then((m) => m.GarageComponent),
      },
      {
        path: 'inspections/new',
        loadComponent: () =>
          import('./features/rider/pages/wizard/inspection-wizard.component').then(
            (m) => m.InspectionWizardComponent
          ),
      },
      {
        path: 'inspections/:id',
        loadComponent: () =>
          import('./features/rider/pages/status/inspection-status.component').then(
            (m) => m.InspectionStatusComponent
          ),
      },
    ],
  },
  {
    path: 'mechanic',
    canActivate: [authGuard, roleGuard(['MECHANIC', 'MANAGER'])],
    children: [
      {
        path: '',
        redirectTo: 'queue',
        pathMatch: 'full',
      },
      {
        path: 'queue',
        loadComponent: () =>
          import('./features/mechanic/pages/t05-queue/t05-queue.component').then(
            (m) => m.T05QueueComponent
          ),
      },
      {
        path: 'workbench/:id',
        loadComponent: () =>
          import('./features/mechanic/pages/t06-workbench/t06-workbench.component').then(
            (m) => m.T06WorkbenchComponent
          ),
      },
      {
        path: 'fleet',
        loadComponent: () =>
          import('./features/mechanic/pages/t07-fleet/t07-fleet.component').then(
            (m) => m.T07FleetComponent
          ),
      },
    ],
  },
  {
    path: 'manager',
    canActivate: [authGuard, roleGuard(['MANAGER'])],
    children: [
      {
        path: '',
        redirectTo: 'approvals',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        redirectTo: 'approvals',
        pathMatch: 'full',
      },
      {
        path: 'approvals',
        loadComponent: () =>
          import('./features/manager/pages/t08-approvals/t08-approvals.component').then(
            (m) => m.T08ApprovalsComponent
          ),
      },
      {
        path: 'catalog',
        loadComponent: () =>
          import('./features/manager/pages/t09-catalog/t09-catalog.component').then(
            (m) => m.T09CatalogComponent
          ),
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./features/manager/pages/t10-analytics/t10-analytics.component').then(
            (m) => m.T10AnalyticsComponent
          ),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
