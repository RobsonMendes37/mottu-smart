"""Internal API router for Java ↔ Python communication (docs/02 §6.5, UC-11)."""
from datetime import datetime, timezone
from typing import List
from fastapi import APIRouter, Depends
from app.application.dtos.catalog_dto import CatalogSyncRequestDTO, CatalogSyncResponseDTO
from app.application.dtos.feedback_dto import FeedbackPolishRequestDTO, FeedbackPolishResponseDTO
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.domain.vtex_normalizer import normalize_vtex_item, parse_motorcycle_model
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
    raw_items, used_fallback = await miner.fetch_catalog(force_refresh=request.forceRefresh)

    postgres_adapter = PostgresPgVectorAdapter()
    vision_adapter = GeminiVisionAdapter()

    items_to_upsert: List[PartCatalogItem] = []
    for raw in raw_items:
        raw_model = raw.get("motorcycle_model")
        if raw_model:
            model_enum = parse_motorcycle_model(raw_model) or MotorcycleModel.MOTTU_SPORT_110I
            norm_item = normalize_vtex_item(raw, model_enum)
            emb = await vision_adapter.generate_embedding(norm_item.part_name)
            norm_item = PartCatalogItem(
                vtex_sku=norm_item.vtex_sku,
                motorcycle_model=norm_item.motorcycle_model,
                part_name=norm_item.part_name,
                category=norm_item.category,
                unit_price_brl=norm_item.unit_price_brl,
                is_natural_wear=norm_item.is_natural_wear,
                price_overridden=norm_item.price_overridden,
                image_url=norm_item.image_url,
                embedding=emb,
            )
            items_to_upsert.append(norm_item)
        else:
            for m in MotorcycleModel:
                norm_item = normalize_vtex_item(raw, m)
                emb = await vision_adapter.generate_embedding(norm_item.part_name)
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
        message="Catálogo sincronizado com sucesso.",
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
