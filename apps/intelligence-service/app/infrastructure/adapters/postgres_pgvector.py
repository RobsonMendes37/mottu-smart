"""PostgreSQL pgvector adapter for catalog parts storage and similarity search (Law 8)."""
import logging
from typing import List, Optional, Tuple
import asyncpg
from app.application.ports.catalog_repository_port import CatalogRepositoryPort
from app.core.config import settings
from app.domain.crag_evaluator import compute_cosine_similarity
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.modules.catalog_miner.scraper import VTEXCatalogMiner
from app.modules.triage_ai.mock_gemini import MockGeminiService

logger = logging.getLogger(__name__)


class PostgresPgVectorAdapter(CatalogRepositoryPort):
    def __init__(self):
        self.host = settings.POSTGRES_HOST
        self.port = settings.POSTGRES_PORT
        self.database = settings.POSTGRES_DB
        self.user = settings.POSTGRES_USER
        self.password = settings.POSTGRES_PASSWORD

    async def _get_connection(self):
        return await asyncpg.connect(
            host=self.host,
            port=self.port,
            database=self.database,
            user=self.user,
            password=self.password,
            timeout=3.0,
        )

    async def upsert_parts(self, parts: List[PartCatalogItem]) -> int:
        if not parts:
            return 0
        try:
            conn = await self._get_connection()
        except Exception as e:
            logger.warning(f"Database connection failed in upsert_parts ({e}). Operating in memory.")
            return len(parts)

        try:
            query = """
                INSERT INTO catalog_parts (
                    vtex_sku, motorcycle_model, part_name, category, unit_price_brl,
                    is_natural_wear, price_overridden, image_url, embedding,
                    stock_quantity, min_stock_quantity, aisle_bin, updated_at
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9::vector, 18, 8, 'Corredor A • Gaveta 01', NOW())
                ON CONFLICT (vtex_sku, motorcycle_model) DO UPDATE
                SET part_name = EXCLUDED.part_name,
                    category = EXCLUDED.category,
                    unit_price_brl = CASE WHEN catalog_parts.price_overridden THEN catalog_parts.unit_price_brl ELSE EXCLUDED.unit_price_brl END,
                    is_natural_wear = EXCLUDED.is_natural_wear,
                    image_url = EXCLUDED.image_url,
                    embedding = COALESCE(EXCLUDED.embedding, catalog_parts.embedding),
                    stock_quantity = COALESCE(catalog_parts.stock_quantity, 18),
                    min_stock_quantity = COALESCE(catalog_parts.min_stock_quantity, 8),
                    aisle_bin = COALESCE(catalog_parts.aisle_bin, 'Corredor A • Gaveta 01'),
                    updated_at = NOW();
            """
            if len(parts) > 1 and hasattr(conn, "executemany"):
                records = [
                    (
                        part.vtex_sku,
                        part.motorcycle_model.value,
                        part.part_name,
                        part.category,
                        part.unit_price_brl,
                        part.is_natural_wear,
                        part.price_overridden,
                        part.image_url,
                        f"[{','.join(str(x) for x in part.embedding)}]" if part.embedding else None,
                    )
                    for part in parts
                ]
                await conn.executemany(query, records)
                return len(records)

            count = 0
            for part in parts:
                vector_str = f"[{','.join(str(x) for x in part.embedding)}]" if part.embedding else None
                await conn.execute(
                    query,
                    part.vtex_sku,
                    part.motorcycle_model.value,
                    part.part_name,
                    part.category,
                    part.unit_price_brl,
                    part.is_natural_wear,
                    part.price_overridden,
                    part.image_url,
                    vector_str,
                )
                count += 1
            return count
        finally:
            await conn.close()

    async def find_similar_parts(
        self, model: MotorcycleModel, embedding: List[float], limit: int = 5
    ) -> List[PartCatalogItem]:
        scored = await self.find_similar_parts_with_scores(model, embedding, limit)
        return [item for item, _ in scored]

    async def find_similar_parts_with_scores(
        self, model: MotorcycleModel, embedding: List[float], limit: int = 5
    ) -> List[Tuple[PartCatalogItem, float]]:
        try:
            conn = await self._get_connection()
        except Exception as e:
            logger.warning(f"Database connection failed in find_similar_parts ({e}). Using offline catalog snapshot.")
            return self._fallback_find_similar_parts(model, embedding, limit)

        try:
            vector_str = f"[{','.join(str(x) for x in embedding)}]"
            rows = await conn.fetch(
                """
                SELECT vtex_sku, motorcycle_model, part_name, category, unit_price_brl,
                       is_natural_wear, price_overridden, image_url,
                       (1 - (embedding <=> $2::vector)) AS similarity
                FROM catalog_parts
                WHERE motorcycle_model = $1 AND embedding IS NOT NULL
                ORDER BY embedding <=> $2::vector ASC
                LIMIT $3;
                """,
                model.value,
                vector_str,
                limit,
            )
            return [
                (
                    PartCatalogItem(
                        vtex_sku=r["vtex_sku"],
                        motorcycle_model=MotorcycleModel(r["motorcycle_model"]),
                        part_name=r["part_name"],
                        category=r["category"],
                        unit_price_brl=float(r["unit_price_brl"]),
                        is_natural_wear=r["is_natural_wear"],
                        price_overridden=r["price_overridden"],
                        image_url=r["image_url"],
                    ),
                    float(r["similarity"]) if r["similarity"] is not None else 0.0,
                )
                for r in rows
            ]
        finally:
            await conn.close()

    def _fallback_find_similar_parts(
        self, model: MotorcycleModel, embedding: List[float], limit: int = 5
    ) -> List[Tuple[PartCatalogItem, float]]:
        miner = VTEXCatalogMiner()
        items = miner.load_seed_snapshot()
        mock_ai = MockGeminiService()
        scored = []
        for raw in items:
            raw_model = raw.get("motorcycle_model")
            if raw_model != model.value:
                continue
            item = PartCatalogItem(
                vtex_sku=raw["vtex_sku"],
                motorcycle_model=model,
                part_name=raw["part_name"],
                category=raw.get("category", "Geral"),
                unit_price_brl=float(raw.get("unit_price_brl", 0.0)),
                is_natural_wear=bool(raw.get("is_natural_wear", False)),
                price_overridden=bool(raw.get("price_overridden", False)),
                image_url=raw.get("image_url"),
            )
            item_emb = mock_ai.generate_embedding(item.part_name)
            sim = compute_cosine_similarity(embedding, item_emb)
            scored.append((item, sim))

        scored.sort(key=lambda x: x[1], reverse=True)
        return scored[:limit]
