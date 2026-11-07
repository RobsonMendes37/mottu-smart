"""Data transfer objects for Accident Video reporting jobs and results (docs/02 §6.5, UC-15)."""
from typing import List, Optional
from pydantic import BaseModel, Field


class AccidentJobPayloadDTO(BaseModel):
    accidentReportId: str
    correlationId: str
    videoObjectKey: str
    photoObjectKeys: Optional[List[str]] = Field(default_factory=list)
    motorcycleModel: str
    drivability: str
    riderNotes: Optional[str] = None


class AccidentDeduplicatedPartDTO(BaseModel):
    catalogPartId: Optional[str] = None
    partName: str
    unitPriceBrl: float
    quantity: int = 1
    videoTimestampSeconds: Optional[int] = None


class AccidentResultPayloadDTO(BaseModel):
    accidentReportId: str
    correlationId: str
    outcome: str  # "SUCCESS" or "AI_UNAVAILABLE"
    confidence: Optional[float] = None
    structuralRiskAlert: Optional[bool] = False
    aiTechnicalSummary: Optional[str] = None
    deduplicatedParts: Optional[List[AccidentDeduplicatedPartDTO]] = None
