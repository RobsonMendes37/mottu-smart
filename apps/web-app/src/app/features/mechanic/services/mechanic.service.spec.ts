import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { MechanicService } from './mechanic.service';
import {
  MechanicQueueItemDto,
  MechanicInspectionDetailsResponse,
  ResolveInspectionRequest,
  ResolveInspectionResponse,
  PageResponse,
} from '../models/mechanic.models';

describe('MechanicService (T05 & T06 API Client)', () => {
  let service: MechanicService;
  let mockHttp: any;

  beforeEach(() => {
    mockHttp = {
      get: vi.fn(),
      post: vi.fn(),
    };
    service = new MechanicService(mockHttp);
  });

  it('TC-MS-1: getQueue sends correct query parameters', () => {
    const mockPage: PageResponse<MechanicQueueItemDto> = {
      items: [
        {
          inspectionId: 'ins-1',
          plate: 'MOT8K11',
          motorcycleModel: 'MOTTU_SPORT_110I',
          riderName: 'Carlos Silva',
          priorityWeight: 1,
          priorityReason: 'CRITICAL_SAFETY',
          safetyRisk: 'CRITICAL_STOPPED',
          fraudSuspected: false,
          aiConfidence: 0.95,
          aiSuggestedVerdict: 'MAU_USO',
          createdAt: '2026-11-07T07:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    };

    mockHttp.get.mockReturnValue(of(mockPage));

    let result: PageResponse<MechanicQueueItemDto> | undefined;
    service.getQueue('CRITICAL_SAFETY', 'MOT8K11', 0, 20).subscribe((res) => {
      result = res;
    });

    expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/mechanic/queue', {
      params: expect.objectContaining({
        reason: 'CRITICAL_SAFETY',
        plate: 'MOT8K11',
        page: '0',
        size: '20',
      }),
    });
    expect(result?.items.length).toBe(1);
    expect(result?.items[0].priorityReason).toBe('CRITICAL_SAFETY');
  });

  it('TC-MS-2: getInspectionDetails calls correct endpoint with UUID', () => {
    const mockDetails: Partial<MechanicInspectionDetailsResponse> = {
      id: 'ins-uuid-1',
      plate: 'MOT1A23',
      status: 'HUMAN_TRIAGE',
    };
    mockHttp.get.mockReturnValue(of(mockDetails));

    let result: MechanicInspectionDetailsResponse | undefined;
    service.getInspectionDetails('ins-uuid-1').subscribe((res) => {
      result = res;
    });

    expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/mechanic/inspections/ins-uuid-1');
    expect(result?.plate).toBe('MOT1A23');
  });

  it('TC-MS-3: resolveInspection posts verdict, parts and technical notes', () => {
    const payload: ResolveInspectionRequest = {
      finalVerdict: 'MAU_USO',
      catalogPartIds: ['part-uuid-1'],
      mechanicNotes: 'Ralado com entortamento recente',
    };

    const mockResponse: ResolveInspectionResponse = {
      inspectionId: 'ins-uuid-1',
      status: 'HUMAN_RESOLVED',
      finalVerdict: 'MAU_USO',
      totalEstimatedBrl: 60.0,
      customerFeedbackText: 'Identificamos que o manete de freio foi danificado.',
    };

    mockHttp.post.mockReturnValue(of(mockResponse));

    let result: ResolveInspectionResponse | undefined;
    service.resolveInspection('ins-uuid-1', payload).subscribe((res) => {
      result = res;
    });

    expect(mockHttp.post).toHaveBeenCalledWith(
      '/api/v1/mechanic/inspections/ins-uuid-1/resolve',
      payload
    );
    expect(result?.status).toBe('HUMAN_RESOLVED');
    expect(result?.totalEstimatedBrl).toBe(60.0);
  });
});
