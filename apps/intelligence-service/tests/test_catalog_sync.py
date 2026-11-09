"""Integration tests for internal catalog sync and feedback polish endpoints (UC-11)."""
from unittest.mock import AsyncMock, patch
import pytest
from fastapi.testclient import TestClient
from app.core.config import settings
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.main import app

client = TestClient(app)


def test_catalog_sync_unauthorized():
    response = client.post("/internal/v1/catalog/sync", json={"forceRefresh": True})
    assert response.status_code == 401 or response.status_code == 422


def test_catalog_sync_invalid_token():
    response = client.post(
        "/internal/v1/catalog/sync",
        headers={"X-Internal-Token": "invalid-token"},
        json={"forceRefresh": True},
    )
    assert response.status_code == 401


def test_catalog_sync_fallback_offline():
    response = client.post(
        "/internal/v1/catalog/sync",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json={"forceRefresh": False},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["syncedCount"] == 24
    assert data["usedFallback"] is True
    assert "syncedAt" in data


@pytest.mark.asyncio
async def test_catalog_sync_preserves_price_overridden():
    """Verify that when a part has price_overridden=True, its price is preserved during sync (UC-11)."""
    from app.infrastructure.adapters.postgres_pgvector import PostgresPgVectorAdapter

    mock_conn = AsyncMock()
    executed_queries = []

    async def fake_execute(query, *args):
        executed_queries.append((query, args))
        return "INSERT 0 1"

    mock_conn.execute = AsyncMock(side_effect=fake_execute)

    adapter = PostgresPgVectorAdapter()
    with patch.object(adapter, "_get_connection", return_value=mock_conn):
        part = PartCatalogItem(
            vtex_sku="SKU-SPT-001",
            motorcycle_model=MotorcycleModel.MOTTU_SPORT_110I,
            part_name="Manete de Freio",
            category="Comandos",
            unit_price_brl=99.90,
            is_natural_wear=False,
            price_overridden=True,
        )
        count = await adapter.upsert_parts([part])
        assert count == 1
        assert len(executed_queries) == 1
        query, args = executed_queries[0]
        # Query must contain price_overridden preservation logic
        assert "CASE WHEN catalog_parts.price_overridden THEN catalog_parts.unit_price_brl ELSE EXCLUDED.unit_price_brl END" in query
        assert args[0] == "SKU-SPT-001"
        assert args[5] is False  # is_natural_wear
        assert args[6] is True   # price_overridden


def test_feedback_polish_natural_wear():
    payload = {
        "finalVerdict": "DESGASTE_NATURAL",
        "partNames": ["Lona de Freio Traseira"],
        "totalEstimatedBrl": 0.00,
        "mechanicNotes": "Desgaste natural por quilometragem.",
    }
    response = client.post(
        "/internal/v1/feedback/polish",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json=payload,
    )
    assert response.status_code == 200
    data = response.json()
    msg = data["customerFriendlyMessage"]
    assert "desgaste natural" in msg.lower()
    assert "0,00" in msg or "0.00" in msg


def test_feedback_polish_bad_use():
    payload = {
        "finalVerdict": "MAU_USO",
        "partNames": ["Manete de Freio"],
        "totalEstimatedBrl": 60.00,
        "mechanicNotes": "Quebra por queda lateral recente.",
    }
    response = client.post(
        "/internal/v1/feedback/polish",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json=payload,
    )
    assert response.status_code == 200
    data = response.json()
    msg = data["customerFriendlyMessage"]
    assert "60,00" in msg or "60.00" in msg
    assert "Quebra por queda lateral recente" in msg


def test_feedback_polish_unauthorized():
    payload = {
        "finalVerdict": "MAU_USO",
        "partNames": ["Manete"],
        "totalEstimatedBrl": 50.00,
        "mechanicNotes": "Dano",
    }
    response = client.post("/internal/v1/feedback/polish", json=payload)
    assert response.status_code == 401 or response.status_code == 422


def test_catalog_sync_skips_items_without_fleet_compatibility():
    raw_items = [
        {"productName": "KIT EMBREAGEM COMPLETA - Sport KS/ESD", "productId": "930", "price": 274.73},
        {"productName": "CAPACETE ABERTO", "productId": "592", "price": 288.64},
        {"productName": "LONA - Honda POP 110i", "productId": "615", "price": 12.99,
         "Compatibilidade da Peça": ["Honda Pop 110i"]},
    ]
    upserted = []

    async def fake_upsert(self, parts):
        upserted.extend(parts)
        return len(parts)

    with patch("app.modules.catalog_miner.scraper.VTEXCatalogMiner.fetch_catalog",
               new=AsyncMock(return_value=(raw_items, False))), \
         patch("app.infrastructure.adapters.postgres_pgvector.PostgresPgVectorAdapter.upsert_parts",
               new=fake_upsert):
        response = client.post(
            "/internal/v1/catalog/sync",
            headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
            json={"forceRefresh": True},
        )

    assert response.status_code == 200
    assert response.json()["usedFallback"] is False
    pairs = sorted((p.vtex_sku, p.motorcycle_model.value) for p in upserted)
    assert pairs == [("615", "HONDA_POP_110I"), ("930", "MOTTU_SPORT_110I")]
