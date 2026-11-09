export type MotorcycleModel =
  | 'MOTTU_SPORT_110I'
  | 'HONDA_POP_110I'
  | 'TVS_APACHE_160'
  | 'TVS_RAIDER_125';

export type UserRole = 'RIDER' | 'MECHANIC' | 'MANAGER';

export interface PendingUserResponseDto {
  id: string;
  fullName: string;
  cpf: string;
  phone: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export interface ApproveUserRequestDto {
  motorcycleId?: string | null;
}

export interface ApproveUserResponseDto {
  userId: string;
  status: string;
  contractId: string | null;
}

export interface RejectUserRequestDto {
  reason: string;
}

export interface RejectUserResponseDto {
  userId: string;
  status: string;
}

export interface CatalogPartResponseDto {
  id: string;
  vtexSku: string;
  motorcycleModel: MotorcycleModel;
  partName: string;
  category: string;
  unitPriceBrl: number;
  isNaturalWear: boolean;
  priceOverridden: boolean;
  imageUrl?: string | null;
  updatedAt: string;
}

export interface UpdateCatalogPartRequest {
  unitPriceBrl?: number;
  isNaturalWear?: boolean;
}

export interface SyncCatalogResponseDto {
  syncedCount: number;
  usedFallback: boolean;
  syncedAt: string;
  message: string;
}

export interface ManagerSettingsDto {
  standardLaborFeeBrl: number;
}

export interface AnalyticsSummaryResponseDto {
  activeMotorcyclesCount: number;
  autoResolvedPercentage: number;
  currentHumanQueueSize: number;
  estimatedMechanicHoursSaved: number;
  lookerStudioEmbedUrl?: string | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AvailableMotorcycleOption {
  id: string;
  plate: string;
  model: MotorcycleModel;
  yardName?: string;
}

export type ContractPaymentStatus = 'PAID' | 'PENDING_PIX' | 'OVERDUE';

export interface WeeklyBillingContract {
  contractId?: string;
  contractCode: string;
  riderName: string;
  plate: string;
  model: string;
  weeklyPlanBrl: number;
  inspectionChargesBrl: number;
  discountBrl: number;
  deliveryBonusBrl?: number;
  netTotalBrl?: number;
  paymentStatus: ContractPaymentStatus;
  dueDateLabel: string;
}

export interface YardBiSnapshot {
  key: string;
  label: string;
  totalInspections: number;
  aiPrecisionPct: string;
  avgSlaSeconds: string;
  auditedRevenueBrl: string;
  naturalWearCount: number;
  preExistingCount: number;
  badUseCount: number;
}

export interface DailyInspectionBar {
  day: string;
  directPct: number;
  workshopPct: number;
  totalCount: number;
}

export interface VerdictBreakdownRow {
  label: string;
  subtitle: string;
  percentage: number;
  count: number;
  barClass: string;
  dotColor: string;
}

export interface FleetModelAnalyticsRow {
  modelName: string;
  code: string;
  inspectionsCount: number;
  sharePct: number;
  aiDeflectionPct: number;
  avgSlaSeconds: string;
}

export interface AuditedPartAnalyticsRow {
  sku: string;
  partName: string;
  modelName: string;
  isNaturalWear: boolean;
  occurrences: number;
  sharePct: number;
  totalAuditedBrl: string;
}

export interface DetailedAnalyticsDto {
  yardSnapshots: YardBiSnapshot[];
  dailyVolumeBars: DailyInspectionBar[];
  verdictBreakdown: VerdictBreakdownRow[];
  fleetModelBreakdown: FleetModelAnalyticsRow[];
  topAuditedParts: AuditedPartAnalyticsRow[];
}

export interface OpsStockoutAlertDto {
  sku: string;
  partName: string;
  modelName: string;
  currentStock: number;
  minStock: number;
  dailyBurnRate: number;
  daysToStockout: number;
  riskLevel: 'CRITICAL' | 'WARNING' | 'HEALTHY';
  suggestedRestockQty: number;
  unitPriceBrl?: number;
  estimatedCostBrl?: number;
  projectedDemand?: number;
  systemCategory?: string;
}

export interface OpsAdvisorResponseDto {
  yardKey: string;
  yardLabel: string;
  generatedAt: string;
  modelUsed: string;
  demandGrowthPct: number;
  projectedNext7DaysInspections: number;
  executiveHeadline: string;
  growthAnalysisText: string;
  stockoutAlerts: OpsStockoutAlertDto[];
  recommendedActions: string[];
  horizonDays?: number;
  scenario?: string;
  scenarioLabel?: string;
  systemCategory?: string;
  totalRestockBudgetBrl?: number;
}


export interface OpsDataExplorerResponseDto {
  query: string;
  yardKey: string;
  yardLabel: string;
  generatedAt: string;
  modelUsed: string;
  directAnswer: string;
  keyMetricLabel: string;
  keyMetricValue: string;
  keyMetricTrend: string;
  columns: string[];
  rows: string[][];
  suggestedActionLabel: string;
  suggestedActionRoute: string;
}
