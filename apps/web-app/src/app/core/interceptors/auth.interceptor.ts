import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next, authServiceOverride?: any) => {
  const authService = authServiceOverride ?? inject(AuthService);
  const token = authService.accessToken();

  let authReq = req;
  // Attach Bearer token if available and not calling public auth endpoints
  if (
    token &&
    !req.url.includes('/api/v1/auth/login') &&
    !req.url.includes('/api/v1/auth/register') &&
    !req.url.includes('/api/v1/auth/public-config')
  ) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/v1/auth/login')) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
