import { describe, it, expect, vi, beforeEach } from 'vitest';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { AccidentService } from './accident.service';
import { SseClientService } from '../../../core/services/sse-client.service';
import {
  AccidentVideoUploadUrlRequest,
  CreateAccidentReportCommand,
} from '../models/accident.models';

describe('AccidentService', () => {
  let service: AccidentService;
  let httpMock: any;
  let sseMock: any;

  beforeEach(() => {
    httpMock = {
      get: vi.fn(),
      post: vi.fn(),
    };
    sseMock = {
      connect: vi.fn(),
    };
    service = new AccidentService(httpMock as HttpClient, sseMock as SseClientService);
  });

  it('TC-ACC-1: Requests presigned video upload URL', async () => {
    const mockResponse = {
      uploadUrl: 'http://minio/video.mp4',
      videoObjectKey: 'accidents/2026/11/test.mp4',
      expiresInSeconds: 900,
    };
    httpMock.post.mockReturnValue(of(mockResponse));

    const req: AccidentVideoUploadUrlRequest = {
      contentType: 'video/mp4',
      fileSizeBytes: 15000000,
      durationSeconds: 24,
    };

    service.getVideoUploadUrl(req).subscribe((res) => {
      expect(res).toEqual(mockResponse);
      expect(httpMock.post).toHaveBeenCalledWith('/api/v1/rider/accidents/upload-video-url', req);
    });
  });

  it('TC-ACC-2: Creates accident report with idempotency key', async () => {
    const mockResponse = {
      accidentReportId: 'acc-uuid-1',
      status: 'QUEUED_AI',
      motorcycleStatus: 'IN_MAINTENANCE',
    };
    httpMock.post.mockReturnValue(of(mockResponse));

    const command: CreateAccidentReportCommand = {
      videoObjectKey: 'accidents/2026/11/video.mp4',
      videoDurationSeconds: 20,
      videoSizeBytes: 14000000,
      hasInjuries: false,
      involvesThirdParty: false,
      drivability: 'NEEDS_TOWING',
      reportedOdometerKm: 1200,
    };

    service.createAccidentReport(command, 'idempotency-123').subscribe((res) => {
      expect(res).toEqual(mockResponse);
      expect(httpMock.post).toHaveBeenCalled();
    });
  });

  it('TC-ACC-3: Validates video size exceeding 30MB', async () => {
    const hugeBlob = new Blob(['0'.repeat(100)], { type: 'video/mp4' });
    Object.defineProperty(hugeBlob, 'size', { value: 35 * 1024 * 1024 });

    const result = await service.validateVideoDuration(hugeBlob as File);
    expect(result.valid).toBe(false);
    expect(result.error).toContain('30 MB');
  });

  it('TC-ACC-4: Fetches accident details and connects SSE stream', () => {
    httpMock.get.mockReturnValue(of({ id: 'acc-1', status: 'ESTIMATED_PENDING_WORKSHOP' }));
    sseMock.connect.mockReturnValue(of({ event: 'status-change', data: { accidentReportId: 'acc-1', status: 'ESTIMATED_PENDING_WORKSHOP', stepLabel: 'Pronto!' } }));

    service.getAccidentDetails('acc-1').subscribe((res) => {
      expect(res.id).toBe('acc-1');
    });

    service.subscribeAccidentLiveStatus('acc-1').subscribe((status) => {
      expect(status.status).toBe('ESTIMATED_PENDING_WORKSHOP');
    });
  });
});
