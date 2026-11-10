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
export type ReportedOriginContext = 'RECEIVED_LIKE_THIS' | 'NORMAL_USE_WEAR' | 'ACCIDENT_OR_DROP';
export type SafetyRiskLevel = 'SAFE_TO_RIDE' | 'CRITICAL_STOPPED';
export type AppointmentShift = 'MORNING' | 'AFTERNOON';

export interface RentedMotorcycle {
  id: string;
  plate: string;
  model: MotorcycleModel;
  status: MotorcycleStatus;
  currentOdometerKm: number;
  yardName: string;
}

export interface ActiveContract {
  contractId: string;
  pickedUpAt: string;
  initialOdometerKm: number;
  motorcycle: RentedMotorcycle;
  pickupPhotosJson?: string | null;
  pickupValidatedByRider?: boolean;
  pickupValidatedAt?: string | null;
  isShieldActive?: boolean;
  remainingShieldHours?: number;
  remainingShieldKm?: number;
}

export interface RecentInspection {
  id: string;
  status: InspectionStatus;
  finalVerdict: DamageVerdict | null;
  totalEstimatedBrl: number;
  createdAt: string;
}

export interface RiderGarageResponse {
  activeContract: ActiveContract | null;
  openInspectionsCount: number;
  recentInspections: RecentInspection[];
}

export interface UploadUrlRequest {
  contentType: string;
  fileSizeBytes: number;
  inspectionId?: string;
  slot?: string;
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

export interface InspectionItemDetail {
  catalogPartId: string;
  partName: string;
  unitPriceBrl: number;
  addedBy: 'AI' | 'MECHANIC';
}

export interface YardAppointmentInfo {
  appointmentId: string;
  yardId: string;
  yardName: string;
  scheduledDate: string;
  shift: AppointmentShift;
  createdAt: string;
}

export interface SpecialistEvaluator {
  specialistRole: string;
  verdict: DamageVerdict;
  confidence: number;
  reasoning: string;
}

export interface CommitteeEvaluationData {
  consensusVerdict: DamageVerdict;
  consensusScore: string;
  isUnanimous: boolean;
  canIdentifyPartsClearly: boolean;
  requiresPresentialInspection: boolean;
  averageConfidence: number;
  dissentingOpinion?: string | null;
  summary: string;
  evaluators: SpecialistEvaluator[];
}

export interface RiderInspectionDetails {
  id: string;
  status: InspectionStatus;
  photoDownloadUrl: string;
  reportedContext: ReportedOriginContext;
  safetyRisk: SafetyRiskLevel;
  reportedOdometerKm: number;
  riderNotes?: string;
  finalVerdict: DamageVerdict | null;
  aiConfidence: number | null;
  laborFeeBrl: number;
  totalEstimatedBrl: number;
  customerFeedbackText: string | null;
  canContest: boolean;
  contestedOnce: boolean;
  items: InspectionItemDetail[];
  appointment: YardAppointmentInfo | null;
  createdAt: string;
  resolvedAt: string | null;
  committeeEvaluationJson?: string | null;
  presentialInspectionRequested?: boolean;
  presentialRequestReason?: string | null;
}

export interface SseStatusChangeEvent {
  inspectionId: string;
  status: InspectionStatus;
  stepLabel: string;
}

export interface ContestInspectionRequest {
  reason: string;
}

export interface ContestInspectionResponse {
  inspectionId: string;
  status: InspectionStatus;
  priorityReason: string;
}

export interface CreateAppointmentRequest {
  yardId: string;
  scheduledDate: string;
  shift: AppointmentShift;
}

export interface CreateAppointmentResponse {
  appointmentId: string;
  inspectionStatus: InspectionStatus;
  yardName: string;
  scheduledDate: string;
  shift: AppointmentShift;
}

export interface YardResponse {
  id: string;
  name: string;
  city: string;
  address: string;
  active: boolean;
}

export interface DailyDeliverySummary {
  dayLabel: string;
  completedCount: number;
  earningsBrl: number;
  onTimeRate: string;
}

export interface ChargedInspectionDto {
  inspectionId: string;
  plate: string;
  reportedAt: string;
  chargeBrl: number;
  partDescription: string;
}

export interface RiderRentalSummaryDto {
  contractCode: string;
  activePlate: string;
  baseWeeklyPlanBrl: number;
  punctualityDiscountBrl: number;
  deliveryBonusBrl: number;
  inspectionChargesBrl: number;
  weeklyTotalBrl: number;
  paymentStatus: string;
  dueDateLabel: string;
  chargedInspections: ChargedInspectionDto[];
  weeklyDeliveriesTotal: number;
  weeklyDeliveryEarningsBrl: number;
  deliveryGoalTarget: number;
  deliveryDays: DailyDeliverySummary[];
}
