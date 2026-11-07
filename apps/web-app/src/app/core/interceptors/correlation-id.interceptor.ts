import { HttpInterceptorFn } from '@angular/common/http';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = req.headers.get('X-Correlation-Id') || crypto.randomUUID();
  const modifiedReq = req.clone({
    setHeaders: {
      'X-Correlation-Id': correlationId,
    },
  });
  return next(modifiedReq);
};
