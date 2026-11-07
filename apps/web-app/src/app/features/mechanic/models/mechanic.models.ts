export type MotorcycleModel =
  | 'MOTTU_SPORT_110I'
  | 'HONDA_POP_110I'
  | 'TVS_APACHE_160'
  | 'TVS_RAIDER_125';

export type MotorcycleStatus = 'AVAILABLE' | 'RENTED' | 'IN_MAINTENANCE' | 'UNAVAILABLE';

export type InspectionStatus =
  | 'QUEUED_AI'
  | 'INVALID_IMAGE'
  | 'AWAITING_AI_RECOVERY'
  | 'AUTO_RESOLVED'
  | 'HUMAN_TRIAGE'
  | 'HUMAN_RESOLVED'
  | 'SCHEDULED'
  | 'CLOSED';

export type DamageVerdict = 'PRE_EXISTENTE' | 'DESGASTE_NATURAL' | 'MAU_USO';

export type QueuePriorityReason =
  | 'CRITICAL_SAFETY'
  | 'RIDER_CONTESTATION'
  | 'LOW_AI_CONFIDENCE'
  | 'FRAUD_SUSPICION';

export type SafetyRiskLevel = 'SAFE_TO_RIDE' | 'CRITICAL_STOPPED';

export type ReportedOriginContext =
  | 'RECEIVED_LIKE_THIS'
  | 'NORMAL_USE_WEAR'
  | 'ACCIDENT_OR_DROP';

export interface MechanicQueueItemDto {
  inspectionId: string;
  plate: string;
  motorcycleModel: MotorcycleModel;
  riderName: string;
  priorityWeight: number; // 1 -> 4
  priorityReason: QueuePriorityReason;
  safetyRisk: SafetyRiskLevel;
  fraudSuspected: boolean;
  aiConfidence: number;
  aiSuggestedVerdict: DamageVerdict;
  createdAt: string;
}

export interface InspectionItemDetailDto {
  catalogPartId: string;
  partName: string;
  unitPriceBrl: number;
  addedBy: string;
}

export interface MechanicInspectionDetailsResponse {
  id: string;
  status: InspectionStatus;
  plate: string;
  motorcycleModel: MotorcycleModel;
  riderName: string;
  riderPhone: string;
  hoursSincePickup: number | null;
  kmDrivenSincePickup: number | null;
  photoDownloadUrl: string;
  fraudSuspected: boolean;
  fraudReason: string | null;
  reportedContext: ReportedOriginContext;
  safetyRisk: SafetyRiskLevel;
  reportedOdometerKm: number;
  riderNotes: string | null;
  aiSuggestedVerdict: DamageVerdict | null;
  aiConfidence: number | null;
  aiTechnicalSummary: string | null;
  contestedOnce: boolean;
  contestationReason: string | null;
  finalVerdict: DamageVerdict | null;
  laborFeeBrl: number | null;
  totalEstimatedBrl: number | null;
  customerFeedbackText: string | null;
  items: InspectionItemDetailDto[];
  createdAt: string;
  resolvedAt: string | null;
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

export interface FleetMotorcycleResponse {
  id: string;
  plate: string;
  model: MotorcycleModel;
  yardId: string;
  yardName: string;
  status: MotorcycleStatus;
  currentOdometerKm: number;
  activeRiderName: string | null;
}

export interface CreateMotorcycleCommand {
  plate: string;
  model: MotorcycleModel;
  yardId: string;
  currentOdometerKm: number;
}

export interface MotorcycleDetailsResponse {
  id: string;
  plate: string;
  model: MotorcycleModel;
  yardId: string;
  yardName: string;
  status: MotorcycleStatus;
  currentOdometerKm: number;
  activeRiderName: string | null;
  createdAt: string;
}

export interface UpdateMotorcycleStatusCommand {
  status: MotorcycleStatus;
}

export interface YardResponse {
  id: string;
  name: string;
  city: string;
  address: string;
  active: boolean;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DamageBoundingBox {
  label: string;
  confidence: number;
  x: number; // percentage (0..100)
  y: number; // percentage (0..100)
  width: number; // percentage (0..100)
  height: number; // percentage (0..100)
}
