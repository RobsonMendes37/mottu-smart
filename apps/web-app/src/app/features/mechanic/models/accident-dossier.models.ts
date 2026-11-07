import { MotorcycleModel } from '../../rider/models/rider.models';
import {
  AccidentItemDto,
  AccidentReportStatus,
  VehicleDrivability,
} from '../../rider/models/accident.models';

export interface MechanicAccidentQueueItemDto {
  id: string;
  plate: string;
  model: MotorcycleModel;
  riderName: string;
  drivability: VehicleDrivability;
  hasInjuries: boolean;
  totalEstimatedBrl: number;
  itemCount: number;
  status: AccidentReportStatus;
  createdAt: string;
}

export interface MechanicAccidentDetailsResponse {
  id: string;
  status: AccidentReportStatus;
  plate: string;
  model: MotorcycleModel;
  riderName: string;
  riderPhone: string;
  videoDownloadUrl: string;
  photoDownloadUrls: string[];
  drivability: VehicleDrivability;
  hasInjuries: boolean;
  involvesThirdParty: boolean;
  policeReportNumber?: string;
  reportedOdometerKm: number;
  riderNotes?: string;
  riderToMechanicNotes?: string;
  aiConfidence?: number;
  structuralRiskAlert: boolean;
  aiTechnicalSummary?: string;
  partsSubtotalBrl: number;
  laborFeeBrl: number;
  towingFeeBrl: number;
  totalEstimatedBrl: number;
  items: AccidentItemDto[];
  createdAt: string;
  confirmedAt?: string;
  mechanicNotes?: string;
}

export interface ConfirmAccidentReportCommand {
  confirmedPartIds?: string[];
  additionalCatalogPartIds?: string[];
  mechanicNotes?: string;
}

export interface ConfirmAccidentReportResponse {
  accidentReportId: string;
  status: AccidentReportStatus;
  totalEstimatedBrl: number;
  confirmedAt: string;
}
