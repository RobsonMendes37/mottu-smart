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
