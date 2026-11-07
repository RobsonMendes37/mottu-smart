import {
  UserRole,
  AccountStatus,
  MotorcycleModel,
  MotorcycleStatus,
  ReportedOriginContext,
  SafetyRiskLevel,
  DamageVerdict,
  QueuePriorityReason,
  InspectionStatus,
  AppointmentShift,
} from './enums.js';

// Standard RFC 7807 Problem Details
export interface ProblemDetail {
  title: string;
  status: number;
  detail: string;
  correlationId?: string;
  type?: string;
  timestamp?: string;
}

// Standard PageResponse<T>
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 6.1 Public & Auth
export interface PublicConfigResponse {
  publicDemoMode: boolean;
}

export interface RegisterRequest {
  fullName: string;
  cpf: string;
  phone: string;
  email: string;
  password: string;
  role: UserRole;
}

export interface RegisterResponse {
  userId: string;
  status: AccountStatus;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  userId: string;
  role: UserRole;
  fullName: string;
}

export interface YardResponse {
  id: string;
  name: string;
  city: string;
  address: string;
  active?: boolean;
}

// 6.2 Rider Routes
export interface ActiveMotorcycleDto {
  id: string;
  plate: string;
  model: MotorcycleModel;
  status: MotorcycleStatus;
  currentOdometerKm: number;
  yardName: string;
}

export interface ActiveContractDto {
  contractId: string;
  pickedUpAt: string;
  initialOdometerKm: number;
  motorcycle: ActiveMotorcycleDto;
}

export interface RecentInspectionDto {
  id: string;
  status: InspectionStatus;
  finalVerdict?: DamageVerdict;
  totalEstimatedBrl: number;
  createdAt: string;
}

export interface GarageResponse {
  activeContract: ActiveContractDto | null;
  openInspectionsCount: number;
  recentInspections: RecentInspectionDto[];
}

export interface UploadUrlRequest {
  contentType: 'image/jpeg' | 'image/png' | 'image/webp' | string;
  fileSizeBytes: number;
}

export interface UploadUrlResponse {
  uploadUrl: string;
  photoObjectKey: string;
  expiresInSeconds: number;
}

export interface CreateInspectionRequest {
  photoObjectKey: string;
  reportedContext: ReportedOriginContext;
  safetyRisk: SafetyRiskLevel;
  reportedOdometerKm: number;
  riderNotes?: string;
}

export interface CreateInspectionResponse {
  inspectionId: string;
  status: InspectionStatus;
}

export interface InspectionItemDto {
  catalogPartId?: string;
  partName: string;
  unitPriceBrl: number;
  addedBy: 'AI' | 'MECHANIC';
}

export interface YardAppointmentDto {
  appointmentId: string;
  yardId: string;
  yardName: string;
  scheduledDate: string;
  shift: AppointmentShift;
}

export interface InspectionDetailResponse {
  id: string;
  status: InspectionStatus;
  photoDownloadUrl: string;
  reportedContext: ReportedOriginContext;
  safetyRisk: SafetyRiskLevel;
  reportedOdometerKm: number;
  riderNotes?: string;
  finalVerdict?: DamageVerdict;
  aiConfidence?: number;
  laborFeeBrl: number;
  totalEstimatedBrl: number;
  customerFeedbackText?: string;
  canContest: boolean;
  contestedOnce: boolean;
  items: InspectionItemDto[];
  appointment: YardAppointmentDto | null;
  createdAt: string;
  resolvedAt?: string;
}

export interface ContestInspectionRequest {
  reason: string;
}

export interface ContestInspectionResponse {
  inspectionId: string;
  status: InspectionStatus;
  priorityReason: QueuePriorityReason;
}

export interface CreateAppointmentRequest {
  yardId: string;
  scheduledDate: string; // YYYY-MM-DD
  shift: AppointmentShift;
}

export interface CreateAppointmentResponse {
  appointmentId: string;
  inspectionStatus: InspectionStatus;
  yardName: string;
  scheduledDate: string;
  shift: AppointmentShift;
}

// 6.3 Mechanic Routes
export interface MechanicQueueItem {
  inspectionId: string;
  plate: string;
  motorcycleModel: MotorcycleModel;
  riderName: string;
  priorityWeight: number; // 1 to 4
  priorityReason: QueuePriorityReason;
  safetyRisk: SafetyRiskLevel;
  fraudSuspected: boolean;
  aiConfidence?: number;
  aiSuggestedVerdict?: DamageVerdict;
  createdAt: string;
}

export interface MechanicInspectionDetailResponse extends InspectionDetailResponse {
  hoursSincePickup: number;
  kmDrivenSincePickup: number;
  fraudSuspected: boolean;
  fraudReason?: string;
  aiTechnicalSummary?: string;
  aiSuggestedVerdict?: DamageVerdict;
  contestationReason?: string;
}

export interface ResolveInspectionRequest {
  finalVerdict: DamageVerdict;
  catalogPartIds: string[];
  mechanicNotes: string;
}

export interface ResolveInspectionResponse {
  inspectionId: string;
  status: InspectionStatus;
  finalVerdict: DamageVerdict;
  totalEstimatedBrl: number;
  customerFeedbackText: string;
}

export interface MotorcycleDto {
  id: string;
  plate: string;
  model: MotorcycleModel;
  yardId: string;
  yardName: string;
  status: MotorcycleStatus;
  currentOdometerKm: number;
  activeRiderName?: string | null;
}

export interface CreateMotorcycleRequest {
  plate: string;
  model: MotorcycleModel;
  yardId: string;
  currentOdometerKm: number;
}

export interface UpdateMotorcycleStatusRequest {
  status: MotorcycleStatus;
}

// 6.4 Manager Routes
export interface PendingUserDto {
  id: string;
  fullName: string;
  cpf: string;
  phone: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export interface ApproveUserRequest {
  motorcycleId?: string | null;
}

export interface ApproveUserResponse {
  userId: string;
  status: AccountStatus;
  contractId: string | null;
}

export interface RejectUserRequest {
  reason: string;
}

export interface RejectUserResponse {
  userId: string;
  status: AccountStatus;
}

export interface SyncCatalogResponse {
  syncedCount: number;
  usedFallback: boolean;
  syncedAt: string;
  message: string;
}

export interface CatalogPartDto {
  id: string;
  vtexSku: string;
  motorcycleModel: MotorcycleModel;
  partName: string;
  category: string;
  unitPriceBrl: number;
  isNaturalWear: boolean;
  priceOverridden: boolean;
  imageUrl?: string;
  updatedAt: string;
}

export interface UpdatePartRequest {
  unitPriceBrl?: number;
  isNaturalWear?: boolean;
}

export interface SystemSettingsDto {
  standardLaborFeeBrl: number;
}

export interface AnalyticsSummaryResponse {
  activeMotorcyclesCount: number;
  autoResolvedPercentage: number;
  currentHumanQueueSize: number;
  estimatedMechanicHoursSaved: number;
  lookerStudioEmbedUrl: string;
}

// 6.7 Accident Reporting & Video Analysis Contracts
export interface AccidentUploadVideoUrlRequest {
  contentType: string;
  fileSize: number;
  durationSeconds?: number;
}

export interface AccidentUploadVideoUrlResponse {
  uploadUrl: string;
  videoObjectKey: string;
  expiresAt: string;
}

export interface AccidentUploadPhotoUrlRequest {
  contentType: string;
  fileSize: number;
}

export interface AccidentUploadPhotoUrlResponse {
  uploadUrl: string;
  photoObjectKey: string;
  expiresAt: string;
}

export interface CreateAccidentReportRequest {
  videoObjectKey: string;
  photoObjectKeys?: string[];
  hasInjuries: boolean;
  involvesThirdParty: boolean;
  policeReportNumber?: string;
  drivability: 'DRIVABLE' | 'NEEDS_TOWING';
  reportedOdometerKm: number;
  description?: string;
}

export interface CreateAccidentReportResponse {
  accidentReportId: string;
  status: string;
  message: string;
  estimatedAt: string;
}

export interface AccidentBudgetItemDto {
  id: string;
  partName: string;
  unitPriceBrl: number;
  videoTimestampSeconds?: number;
}

export interface RiderAccidentDetailsResponse {
  id: string;
  status: string;
  plate: string;
  model: string;
  hasInjuries: boolean;
  involvesThirdParty: boolean;
  policeReportNumber?: string;
  drivability: 'DRIVABLE' | 'NEEDS_TOWING';
  reportedOdometerKm: number;
  partsSubtotalBrl: number;
  laborFeeBrl: number;
  towingFeeBrl: number;
  totalCostBrl: number;
  items: AccidentBudgetItemDto[];
  estimatedAt: string;
}

export interface MechanicAccidentQueueItem {
  id: string;
  status: string;
  plate: string;
  model: string;
  riderName: string;
  hasInjuries: boolean;
  drivability: string;
  totalCostBrl: number;
  itemsCount: number;
  createdAt: string;
}

export interface MechanicAccidentDetailsResponse extends RiderAccidentDetailsResponse {
  videoDownloadUrl?: string;
  riderName: string;
  riderPhone: string;
  mechanicNotes?: string;
  confirmedAt?: string;
}

export interface ConfirmAccidentRequest {
  confirmedPartIds: string[];
  mechanicNotes?: string;
}

export interface ConfirmAccidentResponse {
  accidentReportId: string;
  status: string;
  confirmedTotalCostBrl: number;
  message: string;
  confirmedAt: string;
}
