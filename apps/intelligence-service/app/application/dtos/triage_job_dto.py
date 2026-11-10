"""Data transfer objects for AI Triage background jobs and results (docs/02 §6.5)."""
from typing import List, Optional
from pydantic import BaseModel


class TriageJobPayloadDTO(BaseModel):
    inspectionId: str
    correlationId: str
    photoObjectKey: str
    motorcycleModel: str
    reportedContext: str
    hoursSincePickup: float
    kmDrivenSincePickup: int
    riderNotes: Optional[str] = None


class MatchedPartDTO(BaseModel):
    catalogPartId: Optional[str] = None
    partName: str
    unitPriceBrl: float
    isNaturalWear: bool = False


class TriageResultPayloadDTO(BaseModel):
    inspectionId: str
    correlationId: str
    outcome: str  # "SUCCESS" or "AI_UNAVAILABLE"
    isValidMotorcyclePhoto: Optional[bool] = None
    confidence: Optional[float] = None
    suggestedVerdict: Optional[str] = None
    matchedParts: Optional[List[MatchedPartDTO]] = None
    technicalSummary: Optional[str] = None
    customerFriendlyMessage: Optional[str] = None
    committeeEvaluation: Optional[dict] = None
