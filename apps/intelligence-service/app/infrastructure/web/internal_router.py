"""Internal API router for Java ↔ Python communication (docs/02 §6.5, UC-11)."""
from datetime import datetime, timezone
from typing import List
from fastapi import APIRouter, Depends
from app.application.dtos.catalog_dto import CatalogSyncRequestDTO, CatalogSyncResponseDTO
from app.application.dtos.feedback_dto import FeedbackPolishRequestDTO, FeedbackPolishResponseDTO
from app.application.dtos.analytics_dto import (
    AnalyticsAdvisorRequestDTO,
    AnalyticsAdvisorResponseDTO,
    DataExplorerQueryRequestDTO,
    DataExplorerQueryResponseDTO,
    StockoutAlertDTO,
)
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.domain.services.analytics_forecaster import (
    build_deterministic_ops_advice,
    calculate_demand_growth,
    evaluate_stockout_risks,
)
from app.domain.services.data_explorer_service import answer_manager_query
from app.domain.vtex_normalizer import extract_compatible_models, normalize_vtex_item, parse_motorcycle_model
from app.infrastructure.adapters.gemini_adapter import GeminiVisionAdapter
from app.infrastructure.adapters.postgres_pgvector import PostgresPgVectorAdapter
from app.infrastructure.security.auth import verify_internal_token
from app.modules.catalog_miner.scraper import VTEXCatalogMiner

router = APIRouter(
    prefix="/internal/v1",
    tags=["Internal Operations"],
    dependencies=[Depends(verify_internal_token)],
)


@router.post("/catalog/sync", response_model=CatalogSyncResponseDTO)
async def sync_catalog(request: CatalogSyncRequestDTO) -> CatalogSyncResponseDTO:
    """Sync catalog with VTEX Mottu Store or fallback snapshot (UC-11)."""
    miner = VTEXCatalogMiner()
    raw_items, used_fallback = await miner.fetch_catalog(
        force_refresh=request.forceRefresh,
        max_items=200,
    )

    postgres_adapter = PostgresPgVectorAdapter()
    vision_adapter = GeminiVisionAdapter()

    items_to_upsert: List[PartCatalogItem] = []
    for raw in raw_items:
        compatible_models = extract_compatible_models(raw)
        for m in compatible_models:
            norm_item = normalize_vtex_item(raw, m)
            # Deterministic fast embedding in mock mode; in live mode, leave None so COALESCE preserves DB vector
            emb = None
            if vision_adapter.mock_mode:
                emb = vision_adapter.mock_service.generate_embedding(norm_item.part_name)

            norm_item = PartCatalogItem(
                vtex_sku=norm_item.vtex_sku,
                motorcycle_model=m,
                part_name=norm_item.part_name,
                category=norm_item.category,
                unit_price_brl=norm_item.unit_price_brl,
                is_natural_wear=norm_item.is_natural_wear,
                price_overridden=norm_item.price_overridden,
                image_url=norm_item.image_url,
                embedding=emb,
            )
            items_to_upsert.append(norm_item)

    await postgres_adapter.upsert_parts(items_to_upsert)

    return CatalogSyncResponseDTO(
        syncedCount=len(raw_items),
        usedFallback=used_fallback,
        syncedAt=datetime.now(timezone.utc),
        message="Catálogo sincronizado com a Loja Mottu com sucesso.",
    )


@router.post("/feedback/polish", response_model=FeedbackPolishResponseDTO)
async def polish_feedback(request: FeedbackPolishRequestDTO) -> FeedbackPolishResponseDTO:
    """Polish mechanic technical notes into empathetic customer message (UC-11)."""
    vision_adapter = GeminiVisionAdapter()
    polished = await vision_adapter.polish_mechanic_feedback(
        final_verdict=request.finalVerdict,
        part_names=request.partNames,
        total_estimated_brl=request.totalEstimatedBrl,
        mechanic_notes=request.mechanicNotes,
    )
    return FeedbackPolishResponseDTO(customerFriendlyMessage=polished)


@router.post("/analytics/advisor", response_model=AnalyticsAdvisorResponseDTO)
async def generate_ops_advice(request: AnalyticsAdvisorRequestDTO) -> AnalyticsAdvisorResponseDTO:
    """Analyze demand growth and evaluate stockout risks for yard operations."""
    growth_info = calculate_demand_growth(
        current_period_count=request.currentPeriodInspections,
        previous_period_count=request.previousPeriodInspections,
    )

    parts_dicts = [
        {
            "sku": p.sku,
            "part_name": p.partName,
            "model_name": p.modelName,
            "stock_quantity": p.stockQuantity,
            "min_stock_quantity": p.minStockQuantity,
            "occurrences_30d": p.occurrences30d,
        }
        for p in request.partsInventory
    ]

    stock_risks = evaluate_stockout_risks(parts_dicts)
    base_advice = build_deterministic_ops_advice(
        yard_label=request.yardLabel,
        growth_info=growth_info,
        stock_risks=stock_risks,
        top_model=request.topMotorcycleModel,
    )

    vision_adapter = GeminiVisionAdapter()
    enriched_advice = await vision_adapter.generate_ops_advisor_insights(
        deterministic_advice=base_advice,
        top_model=request.topMotorcycleModel,
        yard_label=request.yardLabel,
    )

    stockout_dtos = [
        StockoutAlertDTO(
            sku=s["sku"],
            partName=s["part_name"],
            modelName=s["model_name"],
            currentStock=s["stock_quantity"],
            minStock=s["min_stock_quantity"],
            dailyBurnRate=s["daily_burn_rate"],
            daysToStockout=s["days_to_stockout"],
            riskLevel=s["risk_level"],
            suggestedRestockQty=s["suggested_restock_qty"],
        )
        for s in enriched_advice.get("stockout_alerts", [])
    ]

    return AnalyticsAdvisorResponseDTO(
        yardKey=request.yardKey,
        yardLabel=request.yardLabel,
        generatedAt=datetime.now(timezone.utc),
        modelUsed="gemini-3.5-flash-lite",
        demandGrowthPct=float(growth_info["growth_rate_pct"]),
        projectedNext7DaysInspections=int(growth_info["projected_next_7d_count"]),
        executiveHeadline=str(enriched_advice.get("executive_headline")),
        growthAnalysisText=str(enriched_advice.get("growth_analysis_text")),
        stockoutAlerts=stockout_dtos,
        recommendedActions=list(enriched_advice.get("recommended_actions", [])),
    )



@router.post("/analytics/ask", response_model=DataExplorerQueryResponseDTO)
async def ask_analytics_data(request: DataExplorerQueryRequestDTO) -> DataExplorerQueryResponseDTO:
    """Analyze a natural language manager query and return structured Google-grade analytics."""
    raw_answer = answer_manager_query(
        query=request.query,
        yard_label=request.yardLabel,
        context_data=request.contextData,
    )

    return DataExplorerQueryResponseDTO(
        query=request.query,
        yardKey=request.yardKey,
        yardLabel=request.yardLabel,
        generatedAt=datetime.now(timezone.utc),
        modelUsed="gemini-3.5-flash-lite",
        directAnswer=str(raw_answer.get("direct_answer", "")),
        keyMetricLabel=str(raw_answer.get("key_metric_label", "")),
        keyMetricValue=str(raw_answer.get("key_metric_value", "")),
        keyMetricTrend=str(raw_answer.get("key_metric_trend", "")),
        columns=list(raw_answer.get("columns", [])),
        rows=list(raw_answer.get("rows", [])),
        suggestedActionLabel=str(raw_answer.get("suggested_action_label", "")),
        suggestedActionRoute=str(raw_answer.get("suggested_action_route", "")),
    )
