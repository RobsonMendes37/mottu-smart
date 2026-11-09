import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  PendingUserResponseDto,
  ApproveUserRequestDto,
  ApproveUserResponseDto,
  RejectUserRequestDto,
  RejectUserResponseDto,
  CatalogPartResponseDto,
  UpdateCatalogPartRequest,
  SyncCatalogResponseDto,
  ManagerSettingsDto,
  AnalyticsSummaryResponseDto,
  PageResponse,
  MotorcycleModel,
  AvailableMotorcycleOption,
  WeeklyBillingContract,
  DetailedAnalyticsDto,
  OpsAdvisorResponseDto,
  OpsDataExplorerResponseDto,
} from '../models/manager.models';

interface FleetMotorcycleRawItem {
  id: string;
  plate: string;
  model: MotorcycleModel;
  yardName?: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class ManagerService {
  private http: HttpClient;

  constructor(http?: HttpClient) {
    try {
      this.http = http ?? inject(HttpClient);
    } catch {
      this.http = http as HttpClient;
    }
  }

  getPendingUsers(): Observable<PendingUserResponseDto[]> {
    return this.http.get<PendingUserResponseDto[]>('/api/v1/manager/users/pending');
  }

  approveUser(
    id: string,
    request?: ApproveUserRequestDto
  ): Observable<ApproveUserResponseDto> {
    return this.http.post<ApproveUserResponseDto>(
      `/api/v1/manager/users/${id}/approve`,
      request ?? {}
    );
  }

  rejectUser(
    id: string,
    request: RejectUserRequestDto
  ): Observable<RejectUserResponseDto> {
    return this.http.post<RejectUserResponseDto>(
      `/api/v1/manager/users/${id}/reject`,
      request
    );
  }

  getParts(
    model?: MotorcycleModel,
    search?: string,
    page = 0,
    size = 20
  ): Observable<PageResponse<CatalogPartResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (model) {
      params = params.set('model', model);
    }
    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PageResponse<CatalogPartResponseDto>>(
      '/api/v1/manager/catalog/parts',
      { params }
    );
  }

  syncMottuStore(): Observable<SyncCatalogResponseDto> {
    return this.http.post<SyncCatalogResponseDto>(
      '/api/v1/manager/catalog/sync-mottu-store',
      {}
    );
  }

  updatePart(
    id: string,
    request: UpdateCatalogPartRequest
  ): Observable<CatalogPartResponseDto> {
    return this.http.patch<CatalogPartResponseDto>(
      `/api/v1/manager/catalog/parts/${id}`,
      request
    );
  }

  getSettings(): Observable<ManagerSettingsDto> {
    return this.http.get<ManagerSettingsDto>('/api/v1/manager/settings');
  }

  updateSettings(request: ManagerSettingsDto): Observable<ManagerSettingsDto> {
    return this.http.patch<ManagerSettingsDto>(
      '/api/v1/manager/settings',
      request
    );
  }

  getAnalyticsSummary(): Observable<AnalyticsSummaryResponseDto> {
    return this.http.get<AnalyticsSummaryResponseDto>(
      '/api/v1/manager/analytics/summary'
    );
  }

  getAvailableMotorcycles(): Observable<AvailableMotorcycleOption[]> {
    const params = new HttpParams()
      .set('status', 'AVAILABLE')
      .set('size', '100');

    return this.http
      .get<PageResponse<FleetMotorcycleRawItem>>('/api/v1/fleet/motorcycles', { params })
      .pipe(
        map((res) =>
          res.items.map((item) => ({
            id: item.id,
            plate: item.plate,
            model: item.model,
            yardName: item.yardName,
          }))
        )
      );
  }

  getBillingContracts(): Observable<WeeklyBillingContract[]> {
    return this.http.get<WeeklyBillingContract[]>('/api/v1/manager/billing/contracts');
  }

  confirmPixPayment(contractCode: string): Observable<WeeklyBillingContract> {
    return this.http.post<WeeklyBillingContract>(
      `/api/v1/manager/billing/contracts/${contractCode}/confirm-pix`,
      {}
    );
  }

  getDetailedAnalytics(): Observable<DetailedAnalyticsDto> {
    return this.http.get<DetailedAnalyticsDto>('/api/v1/manager/analytics/detailed');
  }

  getAiAdvisorAdvice(
    yardKey: string = 'ALL',
    horizonDays: number = 7,
    systemCategory: string = 'ALL',
    scenario: string = 'NORMAL'
  ): Observable<OpsAdvisorResponseDto> {
    const params = new HttpParams()
      .set('yardKey', yardKey)
      .set('horizonDays', horizonDays.toString())
      .set('systemCategory', systemCategory)
      .set('scenario', scenario);
    return this.http.post<OpsAdvisorResponseDto>(
      '/api/v1/manager/analytics/ai-advisor',
      {},
      { params }
    );
  }

  restockPart(sku: string, delta: number): Observable<any> {
    return this.http.patch(`/api/v1/mechanic/operations/stock/${sku}`, { delta });
  }

  askDataExplorer(query: string, yardKey: string = 'ALL'): Observable<OpsDataExplorerResponseDto> {
    return this.http.post<OpsDataExplorerResponseDto>(
      '/api/v1/manager/analytics/ask',
      { query, yardKey }
    );
  }
}
