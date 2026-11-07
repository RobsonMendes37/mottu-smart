import { describe, it, expect, vi } from 'vitest';
import { correlationIdInterceptor } from './correlation-id.interceptor';
import { HttpRequest } from '@angular/common/http';

describe('correlationIdInterceptor', () => {
  it('TC-CORR-1: Adds X-Correlation-Id header if missing', () => {
    let capturedReq: HttpRequest<any> | null = null;
    const nextFn = vi.fn().mockImplementation((req) => {
      capturedReq = req;
      return null;
    });

    const req = new HttpRequest('GET', '/api/v1/yards');
    correlationIdInterceptor(req, nextFn);

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.has('X-Correlation-Id')).toBe(true);
    expect(capturedReq!.headers.get('X-Correlation-Id')).toBeTruthy();
  });

  it('TC-CORR-2: Preserves existing X-Correlation-Id header if already provided', () => {
    let capturedReq: HttpRequest<any> | null = null;
    const nextFn = vi.fn().mockImplementation((req) => {
      capturedReq = req;
      return null;
    });

    const initialReq = new HttpRequest('GET', '/api/v1/yards');
    const req = initialReq.clone({
      setHeaders: { 'X-Correlation-Id': 'existing-corr-id-123' },
    });
    correlationIdInterceptor(req, nextFn);

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.get('X-Correlation-Id')).toBe('existing-corr-id-123');
  });
});
