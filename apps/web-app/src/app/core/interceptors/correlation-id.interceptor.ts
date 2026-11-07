import { HttpInterceptorFn } from '@angular/common/http';
import { generateUuid } from '../utils/uuid.util';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = req.headers.get('X-Correlation-Id') || generateUuid();
  const modifiedReq = req.clone({
    setHeaders: {
      'X-Correlation-Id': correlationId,
    },
  });
  return next(modifiedReq);
};
