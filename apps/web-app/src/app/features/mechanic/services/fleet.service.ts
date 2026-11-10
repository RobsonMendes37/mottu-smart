import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateMotorcycleCommand,
  FleetMotorcycleResponse,
  MotorcycleDetailsResponse,
  MotorcycleModel,
  MotorcycleStatus,
  PageResponse,
  RecordBaseline360Command,
  UpdateMotorcycleStatusCommand,
  YardResponse,
} from '../models/mechanic.models';

export interface FleetFilter {
  status?: MotorcycleStatus;
  plate?: string;
  yardId?: string;
  model?: MotorcycleModel;
}

@Injectable({ providedIn: 'root' })
export class FleetService {
  private http: HttpClient;

  constructor(http?: HttpClient) {
    try {
      this.http = http ?? inject(HttpClient);
    } catch {
      this.http = http as HttpClient;
    }
  }

  listMotorcycles(
    filter?: FleetFilter,
    page = 0,
    size = 20
  ): Observable<PageResponse<FleetMotorcycleResponse>> {
    const params: Record<string, string> = {
      page: page.toString(),
      size: size.toString(),
    };

    if (filter?.status) {
      params['status'] = filter.status;
    }
    if (filter?.plate && filter.plate.trim().length > 0) {
      params['plate'] = filter.plate.trim();
    }
    if (filter?.yardId) {
      params['yardId'] = filter.yardId;
    }
    if (filter?.model) {
      params['model'] = filter.model;
    }

    return this.http.get<PageResponse<FleetMotorcycleResponse>>('/api/v1/fleet/motorcycles', {
      params,
    });
  }

  registerMotorcycle(command: CreateMotorcycleCommand): Observable<MotorcycleDetailsResponse> {
    return this.http.post<MotorcycleDetailsResponse>('/api/v1/fleet/motorcycles', command);
  }

  getMotorcycleById(id: string): Observable<MotorcycleDetailsResponse> {
    return this.http.get<MotorcycleDetailsResponse>(`/api/v1/fleet/motorcycles/${id}`);
  }

  updateStatus(
    id: string,
    command: UpdateMotorcycleStatusCommand
  ): Observable<MotorcycleDetailsResponse> {
    return this.http.patch<MotorcycleDetailsResponse>(
      `/api/v1/fleet/motorcycles/${id}/status`,
      command
    );
  }

  recordBaseline360(
    id: string,
    command: RecordBaseline360Command
  ): Observable<MotorcycleDetailsResponse> {
    return this.http.post<MotorcycleDetailsResponse>(
      `/api/v1/fleet/motorcycles/${id}/baseline-360`,
      command
    );
  }

  getYards(): Observable<YardResponse[]> {
    return this.http.get<YardResponse[]>('/api/v1/yards');
  }
}
