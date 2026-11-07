import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConfirmAccidentReportCommand,
  ConfirmAccidentReportResponse,
  MechanicAccidentDetailsResponse,
  MechanicAccidentQueueItemDto,
} from '../models/accident-dossier.models';
import { PageResponse } from '../models/mechanic.models';

@Injectable({ providedIn: 'root' })
export class AccidentMechanicService {
  private http: HttpClient;
  private readonly baseUrl = '/api/v1/mechanic/accidents';

  constructor(http?: HttpClient) {
    try {
      this.http = http ?? inject(HttpClient);
    } catch {
      this.http = http as HttpClient;
    }
  }

  getAccidentsQueue(page = 0, size = 20): Observable<PageResponse<MechanicAccidentQueueItemDto>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<MechanicAccidentQueueItemDto>>(this.baseUrl, { params });
  }

  getAccidentDetails(id: string): Observable<MechanicAccidentDetailsResponse> {
    return this.http.get<MechanicAccidentDetailsResponse>(`${this.baseUrl}/${id}`);
  }

  confirmAccident(
    id: string,
    command: ConfirmAccidentReportCommand
  ): Observable<ConfirmAccidentReportResponse> {
    return this.http.post<ConfirmAccidentReportResponse>(`${this.baseUrl}/${id}/confirm`, command);
  }
}

