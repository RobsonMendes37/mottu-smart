import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.models';

export const roleGuard = (allowedRoles: UserRole[]): CanActivateFn => {
  return (
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
    authServiceOverride?: any,
    routerOverride?: any
  ) => {
    const authService = authServiceOverride ?? inject(AuthService);
    const router = routerOverride ?? inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }

    const currentRole = authService.userRole();
    if (currentRole && allowedRoles.includes(currentRole)) {
      return true;
    }

    // Role mismatch: redirect to default authorized landing
    switch (currentRole) {
      case 'MANAGER':
        return router.createUrlTree(['/manager/dashboard']);
      case 'MECHANIC':
        return router.createUrlTree(['/mechanic/queue']);
      case 'RIDER':
        return router.createUrlTree(['/rider/garage']);
      default:
        return router.createUrlTree(['/login']);
    }
  };
};
