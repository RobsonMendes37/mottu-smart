"""DTOs for Analytics Advisor operations."""
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


class PartInventoryItemDTO(BaseModel):
    sku: str
    partName: str
    modelName: str
    stockQuantity: int = Field(ge=0)
    minStockQuantity: int = Field(ge=0, default=8)
    occurrences30d: int = Field(ge=0, default=0)
    unitPriceBrl: float = Field(ge=0.0, default=25.0)


class AnalyticsAdvisorRequestDTO(BaseModel):
    yardKey: str = "ALL"
    yardLabel: str = "Todas as Unidades (SP)"
    currentPeriodInspections: int = Field(ge=0, default=0)
    previousPeriodInspections: int = Field(ge=0, default=0)
    topMotorcycleModel: str = "Mottu Sport 110i"
    partsInventory: List[PartInventoryItemDTO] = Field(default_factory=list)
    horizonDays: int = Field(ge=1, default=7)
    scenario: str = "NORMAL"
    systemCategory: str = "ALL"


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
    unitPriceBrl: float = 25.0
    estimatedCostBrl: float = 0.0
    projectedDemand: int = 0
    systemCategory: str = "OTHER"


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
    horizonDays: int = 7
    scenario: str = "NORMAL"
    scenarioLabel: str = "Operação Normal"
    systemCategory: str = "ALL"
    totalRestockBudgetBrl: float = 0.0



class DataExplorerQueryRequestDTO(BaseModel):
    query: str
    yardKey: str = "ALL"
    yardLabel: str = "Todas as Unidades (SP)"
    contextData: dict = Field(default_factory=dict)
    history: List[dict] = Field(default_factory=list)
    skillId: Optional[str] = None


class DataExplorerQueryResponseDTO(BaseModel):
    query: str
    yardKey: str
    yardLabel: str
    generatedAt: datetime
    modelUsed: str = "monalisa-ops-agent"
    directAnswer: str
    keyMetricLabel: str
    keyMetricValue: str
    keyMetricTrend: str
    columns: List[str]
    rows: List[List[str]]
    suggestedActionLabel: str
    suggestedActionRoute: str
    activeSkill: Optional[str] = None
    isAiOffline: bool = False


class TextToSpeechRequestDTO(BaseModel):
    text: str
    voice: str = "pt-BR-FranciscaNeural"
