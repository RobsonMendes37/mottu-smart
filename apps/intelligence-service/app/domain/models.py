"""Pure Python domain models and dataclasses."""
from dataclasses import dataclass
from typing import List, Optional
from app.domain.enums import MotorcycleModel


@dataclass(frozen=True)
class PartCatalogItem:
    vtex_sku: str
    motorcycle_model: MotorcycleModel
    part_name: str
    category: str
    unit_price_brl: float
    is_natural_wear: bool = False
    price_overridden: bool = False
    image_url: Optional[str] = None
    embedding: Optional[List[float]] = None

    def __post_init__(self):
        if self.unit_price_brl < 0:
            raise ValueError("unit_price_brl cannot be negative")


@dataclass(frozen=True)
class DamageItem:
    part_name: str
    unit_price_brl: float
    catalog_part_id: Optional[str] = None
    is_natural_wear: bool = False


@dataclass(frozen=True)
class CRAGEvaluationResult:
    similarity_score: float
    confidence_adjusted: float
    is_hallucination_suspected: bool
    requires_human_triage: bool
    matched_part: Optional[PartCatalogItem] = None
    triage_reason: Optional[str] = None


@dataclass(frozen=True)
class VideoPartDetection:
    part_name: str
    unit_price_brl: float = 0.0
    catalog_part_id: Optional[str] = None
    video_timestamp_seconds: Optional[int] = None
    confidence: float = 1.0

    def __post_init__(self):
        if self.unit_price_brl < 0:
            raise ValueError("unit_price_brl cannot be negative")


@dataclass(frozen=True)
class DeduplicatedPart:
    catalog_part_id: Optional[str]
    part_name: str
    unit_price_brl: float
    quantity: int
    video_timestamp_seconds: Optional[int]


@dataclass(frozen=True)
class AccidentEvaluationResult:
    confidence: float
    structural_risk_alert: bool
    ai_technical_summary: str
    deduplicated_parts: List[DeduplicatedPart]
    total_parts_count: int
    subtotal_parts_brl: float


