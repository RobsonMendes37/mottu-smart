import { MotorcycleStatus } from './rider.models';

export type AccidentReportStatus =
  | 'QUEUED_AI'
  | 'AWAITING_AI_RECOVERY'
  | 'ESTIMATED_PENDING_WORKSHOP'
  | 'WORKSHOP_CONFIRMED';

export type VehicleDrivability = 'DRIVABLE' | 'NEEDS_TOWING';

export interface AccidentVideoUploadUrlRequest {
  contentType: string;
  fileSizeBytes: number;
  durationSeconds: number;
}

export interface AccidentVideoUploadUrlResponse {
  uploadUrl: string;
  videoObjectKey: string;
  expiresInSeconds: number;
}

export interface AccidentPhotoUploadUrlRequest {
  contentType: string;
  fileSizeBytes: number;
}

export interface AccidentPhotoUploadUrlResponse {
  uploadUrl: string;
  photoObjectKey: string;
  expiresInSeconds: number;
}

export interface CreateAccidentReportCommand {
  videoObjectKey: string;
  videoDurationSeconds: number;
  videoSizeBytes: number;
  photoObjectKeys?: string[];
  hasInjuries: boolean;
  involvesThirdParty: boolean;
  policeReportNumber?: string | null;
  drivability: VehicleDrivability;
  reportedOdometerKm: number;
  riderNotes?: string;
  riderToMechanicNotes?: string;
}

export interface CreateAccidentReportResponse {
  accidentReportId: string;
  status: AccidentReportStatus;
  motorcycleStatus: MotorcycleStatus;
}

export interface AccidentItemDto {
  catalogPartId?: string;
  partName: string;
  unitPriceBrl: number;
  quantity: number;
  videoTimestampSeconds?: number;
}

export interface RiderAccidentReportDetails {
  id: string;
  status: AccidentReportStatus;
  videoDownloadUrl: string;
  drivability: VehicleDrivability;
  hasInjuries: boolean;
  structuralRiskAlert: boolean;
  aiConfidence: number | null;
  partsSubtotalBrl: number;
  laborFeeBrl: number;
  towingFeeBrl: number;
  totalEstimatedBrl: number;
  items: AccidentItemDto[];
  riderToMechanicNotes?: string;
  createdAt: string;
}

export interface AccidentStatusChangeEvent {
  accidentReportId: string;
  status: AccidentReportStatus;
  stepLabel: string;
}
