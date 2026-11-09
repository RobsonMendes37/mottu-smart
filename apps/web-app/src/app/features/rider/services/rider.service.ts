import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RiderGarageResponse,
  UploadUrlRequest,
  UploadUrlResponse,
  CreateInspectionRequest,
  CreateInspectionResponse,
  RiderInspectionDetails,
  ContestInspectionRequest,
  ContestInspectionResponse,
  CreateAppointmentRequest,
  CreateAppointmentResponse,
  YardResponse,
  RiderRentalSummaryDto,
} from '../models/rider.models';

@Injectable({ providedIn: 'root' })
export class RiderService {
  private http: HttpClient;

  constructor(http?: HttpClient) {
    try {
      this.http = http ?? inject(HttpClient);
    } catch {
      this.http = http as HttpClient;
    }
  }

  getGarage(): Observable<RiderGarageResponse> {
    return this.http.get<RiderGarageResponse>('/api/v1/rider/garage');
  }

  getUploadUrl(request: UploadUrlRequest): Observable<UploadUrlResponse> {
    return this.http.post<UploadUrlResponse>('/api/v1/rider/inspections/upload-url', request);
  }

  createInspection(
    idempotencyKey: string,
    request: CreateInspectionRequest
  ): Observable<CreateInspectionResponse> {
    const headers = new HttpHeaders({
      'Idempotency-Key': idempotencyKey,
    });
    return this.http.post<CreateInspectionResponse>('/api/v1/rider/inspections', request, {
      headers,
    });
  }

  getInspectionDetails(id: string): Observable<RiderInspectionDetails> {
    return this.http.get<RiderInspectionDetails>(`/api/v1/rider/inspections/${id}`);
  }

  contestInspection(
    id: string,
    request: ContestInspectionRequest
  ): Observable<ContestInspectionResponse> {
    return this.http.post<ContestInspectionResponse>(
      `/api/v1/rider/inspections/${id}/contest`,
      request
    );
  }

  createAppointment(
    id: string,
    request: CreateAppointmentRequest
  ): Observable<CreateAppointmentResponse> {
    return this.http.post<CreateAppointmentResponse>(
      `/api/v1/rider/inspections/${id}/appointment`,
      request
    );
  }

  getYards(): Observable<YardResponse[]> {
    return this.http.get<YardResponse[]>('/api/v1/yards');
  }

  getRentalSummary(): Observable<RiderRentalSummaryDto> {
    return this.http.get<RiderRentalSummaryDto>('/api/v1/rider/rental/summary');
  }

  payRentalPix(): Observable<RiderRentalSummaryDto> {
    return this.http.post<RiderRentalSummaryDto>('/api/v1/rider/rental/pay-pix', {});
  }

  registerDailyDeliveries(
    completedCount: number,
    earningsBrl: number
  ): Observable<RiderRentalSummaryDto> {
    return this.http.post<RiderRentalSummaryDto>('/api/v1/rider/rental/deliveries', {
      completedCount,
      earningsBrl,
    });
  }
}
