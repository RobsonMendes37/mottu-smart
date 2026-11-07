import crypto from 'node:crypto';
import {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  YardResponse,
  GarageResponse,
  UploadUrlRequest,
  UploadUrlResponse,
  CreateInspectionRequest,
  CreateInspectionResponse,
  InspectionDetailResponse,
  ContestInspectionRequest,
  ContestInspectionResponse,
  CreateAppointmentRequest,
  CreateAppointmentResponse,
  MechanicQueueItem,
  MechanicInspectionDetailResponse,
  ResolveInspectionRequest,
  ResolveInspectionResponse,
  PageResponse,
  MotorcycleDto,
  CreateMotorcycleRequest,
  PendingUserDto,
  ApproveUserRequest,
  ApproveUserResponse,
  RejectUserRequest,
  RejectUserResponse,
  SyncCatalogResponse,
  CatalogPartDto,
  UpdatePartRequest,
  SystemSettingsDto,
  AnalyticsSummaryResponse,
  PublicConfigResponse,
} from '../contracts/types.js';

export interface ApiResponse<T> {
  status: number;
  data: T;
  headers: Headers;
  ok: boolean;
}

export class MottuApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string = 'http://localhost:18088') {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  public setToken(token: string | null) {
    this.token = token;
  }

  public async request<T = any>(
    path: string,
    options: {
      method?: string;
      body?: any;
      headers?: Record<string, string>;
      idempotencyKey?: string;
    } = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${path.startsWith('/') ? path : '/' + path}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-Correlation-Id': crypto.randomUUID(),
      ...(options.headers || {}),
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    if (options.idempotencyKey) {
      headers['Idempotency-Key'] = options.idempotencyKey;
    }

    const res = await fetch(url, {
      method: options.method || 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    });

    let data: any = null;
    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
      data = await res.json();
    } else {
      data = await res.text();
    }

    return {
      status: res.status,
      data,
      headers: res.headers,
      ok: res.ok,
    };
  }

  // 6.1 Public & Auth
  public async getPublicConfig(): Promise<ApiResponse<PublicConfigResponse>> {
    return this.request('/api/v1/auth/public-config');
  }

  public async register(req: RegisterRequest): Promise<ApiResponse<RegisterResponse>> {
    return this.request('/api/v1/auth/register', { method: 'POST', body: req });
  }

  public async login(req: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    const res = await this.request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body: req });
    if (res.ok && res.data.accessToken) {
      this.token = res.data.accessToken;
    }
    return res;
  }

  public async getYards(): Promise<ApiResponse<YardResponse[]>> {
    return this.request('/api/v1/yards');
  }

  // 6.2 Rider Routes
  public async getGarage(): Promise<ApiResponse<GarageResponse>> {
    return this.request('/api/v1/rider/garage');
  }

  public async getUploadUrl(req: UploadUrlRequest): Promise<ApiResponse<UploadUrlResponse>> {
    return this.request('/api/v1/rider/inspections/upload-url', { method: 'POST', body: req });
  }

  public async createInspection(
    req: CreateInspectionRequest,
    idempotencyKey?: string
  ): Promise<ApiResponse<CreateInspectionResponse>> {
    return this.request('/api/v1/rider/inspections', {
      method: 'POST',
      body: req,
      idempotencyKey: idempotencyKey || crypto.randomUUID(),
    });
  }

  public async getInspection(id: string): Promise<ApiResponse<InspectionDetailResponse>> {
    return this.request(`/api/v1/rider/inspections/${id}`);
  }

  public async contestInspection(
    id: string,
    req: ContestInspectionRequest
  ): Promise<ApiResponse<ContestInspectionResponse>> {
    return this.request(`/api/v1/rider/inspections/${id}/contest`, { method: 'POST', body: req });
  }

  public async createAppointment(
    id: string,
    req: CreateAppointmentRequest
  ): Promise<ApiResponse<CreateAppointmentResponse>> {
    return this.request(`/api/v1/rider/inspections/${id}/appointment`, { method: 'POST', body: req });
  }

  // 6.3 Mechanic Routes
  public async getMechanicQueue(params: {
    reason?: string;
    plate?: string;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<MechanicQueueItem>>> {
    const q = new URLSearchParams();
    if (params.reason) q.set('reason', params.reason);
    if (params.plate) q.set('plate', params.plate);
    if (params.page !== undefined) q.set('page', String(params.page));
    if (params.size !== undefined) q.set('size', String(params.size));
    const query = q.toString() ? `?${q.toString()}` : '';
    return this.request(`/api/v1/mechanic/queue${query}`);
  }

  public async getMechanicInspectionDetail(id: string): Promise<ApiResponse<MechanicInspectionDetailResponse>> {
    return this.request(`/api/v1/mechanic/inspections/${id}`);
  }

  public async resolveInspection(
    id: string,
    req: ResolveInspectionRequest
  ): Promise<ApiResponse<ResolveInspectionResponse>> {
    return this.request(`/api/v1/mechanic/inspections/${id}/resolve`, { method: 'POST', body: req });
  }

  public async getFleet(params: { status?: string; plate?: string } = {}): Promise<ApiResponse<PageResponse<MotorcycleDto>>> {
    return this.request('/api/v1/fleet/motorcycles');
  }

  public async createMotorcycle(req: CreateMotorcycleRequest): Promise<ApiResponse<MotorcycleDto>> {
    return this.request('/api/v1/fleet/motorcycles', { method: 'POST', body: req });
  }

  // 6.4 Manager Routes
  public async getPendingUsers(): Promise<ApiResponse<PendingUserDto[]>> {
    return this.request('/api/v1/manager/users/pending');
  }

  public async approveUser(id: string, req: ApproveUserRequest): Promise<ApiResponse<ApproveUserResponse>> {
    return this.request(`/api/v1/manager/users/${id}/approve`, { method: 'POST', body: req });
  }

  public async rejectUser(id: string, req: RejectUserRequest): Promise<ApiResponse<RejectUserResponse>> {
    return this.request(`/api/v1/manager/users/${id}/reject`, { method: 'POST', body: req });
  }

  public async syncCatalog(): Promise<ApiResponse<SyncCatalogResponse>> {
    return this.request('/api/v1/manager/catalog/sync-mottu-store', { method: 'POST' });
  }

  public async getCatalogParts(): Promise<ApiResponse<PageResponse<CatalogPartDto>>> {
    return this.request('/api/v1/manager/catalog/parts');
  }

  public async updateCatalogPart(id: string, req: UpdatePartRequest): Promise<ApiResponse<CatalogPartDto>> {
    return this.request(`/api/v1/manager/catalog/parts/${id}`, { method: 'PATCH', body: req });
  }

  public async getSettings(): Promise<ApiResponse<SystemSettingsDto>> {
    return this.request('/api/v1/manager/settings');
  }

  public async updateSettings(req: SystemSettingsDto): Promise<ApiResponse<SystemSettingsDto>> {
    return this.request('/api/v1/manager/settings', { method: 'PATCH', body: req });
  }

  public async getAnalyticsSummary(): Promise<ApiResponse<AnalyticsSummaryResponse>> {
    return this.request('/api/v1/manager/analytics/summary');
  }
}
