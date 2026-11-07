import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { generateUuid } from '../utils/uuid.util';

export interface SseEvent<T = unknown> {
  event: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class SseClientService {
  private authService: AuthService | null;

  constructor(authService?: AuthService) {
    try {
      this.authService = authService ?? inject(AuthService, { optional: true });
    } catch {
      this.authService = authService ?? null;
    }
  }

  /**
   * Connects to a Server-Sent Events stream using fetch with ReadableStream.
   * Transmits Authorization Bearer header and X-Correlation-Id.
   */
  connect<T = unknown>(url: string): Observable<SseEvent<T>> {
    return new Observable((observer) => {
      const abortController = new AbortController();
      const token = this.authService?.accessToken() ?? null;
      const correlationId = generateUuid();

      const headers: Record<string, string> = {
        Accept: 'text/event-stream',
        'Cache-Control': 'no-cache',
        'X-Correlation-Id': correlationId,
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      fetch(url, {
        method: 'GET',
        headers,
        signal: abortController.signal,
      })
        .then(async (response) => {
          if (!response.ok) {
            throw new Error(`SSE HTTP error: ${response.status} ${response.statusText}`);
          }
          if (!response.body) {
            throw new Error('ReadableStream not supported on this response body');
          }

          const reader = response.body.getReader();
          const decoder = new TextDecoder('utf-8');
          let buffer = '';

          while (true) {
            const { done, value } = await reader.read();
            if (done) {
              break;
            }

            buffer += decoder.decode(value, { stream: true });
            const messages = buffer.split('\n\n');
            buffer = messages.pop() ?? '';

            for (const rawMessage of messages) {
              if (!rawMessage.trim()) continue;

              const lines = rawMessage.split('\n');
              let eventType = 'message';
              let dataStr = '';

              for (const line of lines) {
                if (line.startsWith('event:')) {
                  eventType = line.replace('event:', '').trim();
                } else if (line.startsWith('data:')) {
                  dataStr += line.replace('data:', '').trim();
                }
              }

              if (dataStr) {
                try {
                  const parsed = JSON.parse(dataStr);
                  observer.next({ event: eventType, data: parsed as T });
                } catch {
                  observer.next({ event: eventType, data: dataStr as unknown as T });
                }
              }
            }
          }
          observer.complete();
        })
        .catch((err: Error) => {
          if (err.name !== 'AbortError') {
            observer.error(err);
          }
        });

      // Cleanup upon unsubscription
      return () => {
        abortController.abort();
      };
    });
  }
}
