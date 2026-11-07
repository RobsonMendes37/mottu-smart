import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SseClientService } from './sse-client.service';

describe('SseClientService', () => {
  let service: SseClientService;
  let mockAuthService: any;

  beforeEach(() => {
    mockAuthService = {
      accessToken: vi.fn().mockReturnValue('mock.jwt.token'),
    };
    service = new (SseClientService as any)();
    (service as any).authService = mockAuthService;
  });

  it('TC-SSE-1: Connect triggers authenticated fetch with Accept text/event-stream', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader: () => ({
          read: vi.fn().mockResolvedValue({ done: true, value: undefined }),
        }),
      },
    });
    vi.stubGlobal('fetch', mockFetch);

    const sub = service.connect('/api/v1/rider/inspections/uuid-1/stream').subscribe();

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/rider/inspections/uuid-1/stream',
      expect.objectContaining({
        headers: expect.objectContaining({
          Accept: 'text/event-stream',
          Authorization: 'Bearer mock.jwt.token',
        }),
      })
    );

    sub.unsubscribe();
    vi.unstubAllGlobals();
  });

  it('TC-SSE-2: Unsubscribing aborts the fetch request', async () => {
    let capturedSignal: AbortSignal | null = null;
    const mockFetch = vi.fn().mockImplementation((url, init) => {
      capturedSignal = init?.signal ?? null;
      return new Promise(() => {}); // never resolves
    });
    vi.stubGlobal('fetch', mockFetch);

    const sub = service.connect('/api/v1/rider/inspections/uuid-1/stream').subscribe();
    expect(capturedSignal).not.toBeNull();
    expect(capturedSignal!.aborted).toBe(false);

    sub.unsubscribe();
    expect(capturedSignal!.aborted).toBe(true);

    vi.unstubAllGlobals();
  });

  it('TC-SSE-3: Correctly parses SSE messages and emits events', async () => {
    const ssePayload = 'event: status-change\ndata: {"inspectionId": "insp-1", "status": "APPROVED"}\n\n';
    const encoder = new TextEncoder();
    const encodedChunk = encoder.encode(ssePayload);

    let readCount = 0;
    const mockReader = {
      read: vi.fn().mockImplementation(() => {
        if (readCount === 0) {
          readCount++;
          return Promise.resolve({ done: false, value: encodedChunk });
        }
        return Promise.resolve({ done: true, value: undefined });
      }),
    };

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader: () => mockReader,
      },
    });
    vi.stubGlobal('fetch', mockFetch);

    const events: any[] = [];
    service.connect<any>('/api/v1/inspections/stream').subscribe({
      next: (evt) => events.push(evt),
    });

    // Wait for microtasks
    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(events.length).toBe(1);
    expect(events[0].event).toBe('status-change');
    expect(events[0].data).toEqual({ inspectionId: 'insp-1', status: 'APPROVED' });

    vi.unstubAllGlobals();
  });
});
