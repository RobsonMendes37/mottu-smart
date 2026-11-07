import { describe, it, expect, beforeEach, vi } from 'vitest';
import { RiderService } from './rider.service';
import { of, throwError } from 'rxjs';
import {
  CreateInspectionRequest,
  CreateAppointmentRequest,
  ContestInspectionRequest,
  UploadUrlRequest,
} from '../models/rider.models';

describe('RiderService', () => {
  let service: RiderService;
  let mockHttp: any;

  beforeEach(() => {
    mockHttp = {
      get: vi.fn(),
      post: vi.fn(),
    };
    service = new RiderService(mockHttp);
  });

  it('TC-RDS-1: getGarage sends GET /api/v1/rider/garage', () => {
    const mockResponse = {
      activeContract: null,
      openInspectionsCount: 0,
      recentInspections: [],
    };
    mockHttp.get.mockReturnValue(of(mockResponse));

    let result: any;
    service.getGarage().subscribe((res) => (result = res));

    expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/rider/garage');
    expect(result).toEqual(mockResponse);
  });

  it('TC-RDS-2: getUploadUrl sends POST /api/v1/rider/inspections/upload-url', () => {
    const req: UploadUrlRequest = {
      contentType: 'image/webp',
      fileSizeBytes: 204800,
      slot: 'damage',
    };
    const mockRes = {
      uploadUrl: 'http://minio/bucket/presigned-put-url',
      photoObjectKey: 'inspections/temp/uuid.webp',
      expiresInSeconds: 300,
    };
    mockHttp.post.mockReturnValue(of(mockRes));

    let result: any;
    service.getUploadUrl(req).subscribe((res) => (result = res));

    expect(mockHttp.post).toHaveBeenCalledWith(
      '/api/v1/rider/inspections/upload-url',
      req
    );
    expect(result).toEqual(mockRes);
  });

  it('TC-RDS-3: createInspection sends POST with Idempotency-Key header', () => {
    const idempotencyKey = '123e4567-e89b-12d3-a456-426614174000';
    const req: CreateInspectionRequest = {
      photoObjectKey: 'inspections/temp/uuid.webp',
      reportedContext: 'RECEIVED_LIKE_THIS',
      safetyRisk: 'SAFE_TO_RIDE',
      reportedOdometerKm: 1035,
    };
    const mockRes = {
      inspectionId: 'ins-uuid-1',
      status: 'QUEUED_AI',
    };
    mockHttp.post.mockReturnValue(of(mockRes));

    let result: any;
    service.createInspection(idempotencyKey, req).subscribe((res) => (result = res));

    expect(mockHttp.post).toHaveBeenCalled();
    const [url, body, options] = mockHttp.post.mock.calls[0];
    expect(url).toBe('/api/v1/rider/inspections');
    expect(body).toEqual(req);
    expect(options.headers.get('Idempotency-Key')).toBe(idempotencyKey);
    expect(result).toEqual(mockRes);
  });

  it('TC-RDS-4: getInspectionDetails sends GET /api/v1/rider/inspections/{id}', () => {
    const id = 'ins-uuid-1';
    const mockRes = {
      id,
      status: 'AUTO_RESOLVED',
      totalEstimatedBrl: 0,
      canContest: true,
      contestedOnce: false,
    };
    mockHttp.get.mockReturnValue(of(mockRes));

    let result: any;
    service.getInspectionDetails(id).subscribe((res) => (result = res));

    expect(mockHttp.get).toHaveBeenCalledWith(`/api/v1/rider/inspections/${id}`);
    expect(result).toEqual(mockRes);
  });

  it('TC-RDS-5: contestInspection sends POST /api/v1/rider/inspections/{id}/contest', () => {
    const id = 'ins-uuid-1';
    const req: ContestInspectionRequest = {
      reason: 'A peça já estava danificada quando retirei a moto no galpão Butantã.',
    };
    const mockRes = {
      inspectionId: id,
      status: 'HUMAN_TRIAGE',
      priorityReason: 'RIDER_CONTESTATION',
    };
    mockHttp.post.mockReturnValue(of(mockRes));

    let result: any;
    service.contestInspection(id, req).subscribe((res) => (result = res));

    expect(mockHttp.post).toHaveBeenCalledWith(
      `/api/v1/rider/inspections/${id}/contest`,
      req
    );
    expect(result).toEqual(mockRes);
  });

  it('TC-RDS-6: createAppointment sends POST /api/v1/rider/inspections/{id}/appointment', () => {
    const id = 'ins-uuid-1';
    const req: CreateAppointmentRequest = {
      yardId: 'yard-butanta',
      scheduledDate: '2026-11-10',
      shift: 'MORNING',
    };
    const mockRes = {
      appointmentId: 'apt-uuid-1',
      inspectionStatus: 'SCHEDULED',
      yardName: 'Galpão Butantã',
      scheduledDate: '2026-11-10',
      shift: 'MORNING',
    };
    mockHttp.post.mockReturnValue(of(mockRes));

    let result: any;
    service.createAppointment(id, req).subscribe((res) => (result = res));

    expect(mockHttp.post).toHaveBeenCalledWith(
      `/api/v1/rider/inspections/${id}/appointment`,
      req
    );
    expect(result).toEqual(mockRes);
  });

  it('TC-RDS-7: getYards sends GET /api/v1/yards', () => {
    const mockRes = [
      { id: 'yard-1', name: 'Butantã', city: 'São Paulo', address: 'Av Vital Brasil', active: true },
    ];
    mockHttp.get.mockReturnValue(of(mockRes));

    let result: any;
    service.getYards().subscribe((res) => (result = res));

    expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/yards');
    expect(result).toEqual(mockRes);
  });
});
