"""DTOs for Analytics Advisor operations."""
from datetime import datetime
from typing import List
from pydantic import BaseModel, Field


class PartInventoryItemDTO(BaseModel):
    sku: str
    partName: str
    modelName: str
    stockQuantity: int = Field(ge=0)
    minStockQuantity: int = Field(ge=0, default=8)
    occurrences30d: int = Field(ge=0, default=0)


class AnalyticsAdvisorRequestDTO(BaseModel):
    yardKey: str = "ALL"
    yardLabel: str = "Todas as Unidades (SP)"
    currentPeriodInspections: int = Field(ge=0, default=0)
    previousPeriodInspections: int = Field(ge=0, default=0)
    topMotorcycleModel: str = "Mottu Sport 110i"
    partsInventory: List[PartInventoryItemDTO] = Field(default_factory=list)


class StockoutAlertDTO(BaseModel):
    sku: str
    partName: str
    modelName: str
    currentStock: int
    minStock: int
    dailyBurnRate: float
    daysToStockout: int
    riskLevel: str
    suggestedRestockQty: int


class AnalyticsAdvisorResponseDTO(BaseModel):
    yardKey: str
    yardLabel: str
    generatedAt: datetime
    modelUsed: str
    demandGrowthPct: float
    projectedNext7DaysInspections: int
    executiveHeadline: str
    growthAnalysisText: str
    stockoutAlerts: List[StockoutAlertDTO]
    recommendedActions: List[str]

