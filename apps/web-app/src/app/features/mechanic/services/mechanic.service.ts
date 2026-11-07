import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MechanicQueueItemDto,
  MechanicInspectionDetailsResponse,
  ResolveInspectionRequest,
  ResolveInspectionResponse,
  PageResponse,
  QueuePriorityReason,
} from '../models/mechanic.models';

@Injectable({ providedIn: 'root' })
export class MechanicService {
  private http: HttpClient;

  constructor(http?: HttpClient) {
    try {
      this.http = http ?? inject(HttpClient);
    } catch {
      this.http = http as HttpClient;
    }
  }

  getQueue(
    reason?: QueuePriorityReason,
    plate?: string,
    page = 0,
    size = 20
  ): Observable<PageResponse<MechanicQueueItemDto>> {
    const params: Record<string, string> = {
      page: page.toString(),
      size: size.toString(),
    };

    if (reason) {
      params['reason'] = reason;
    }
    if (plate && plate.trim().length > 0) {
      params['plate'] = plate.trim();
    }

    return this.http.get<PageResponse<MechanicQueueItemDto>>('/api/v1/mechanic/queue', {
      params,
    });
  }

  getInspectionDetails(id: string): Observable<MechanicInspectionDetailsResponse> {
    return this.http.get<MechanicInspectionDetailsResponse>(
      `/api/v1/mechanic/inspections/${id}`
    );
  }

  resolveInspection(
    id: string,
    request: ResolveInspectionRequest
  ): Observable<ResolveInspectionResponse> {
    return this.http.post<ResolveInspectionResponse>(
      `/api/v1/mechanic/inspections/${id}/resolve`,
      request
    );
  }
}
