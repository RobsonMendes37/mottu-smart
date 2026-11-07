"""Integration and behavior tests for the Redis background triage worker (docs/02 §6.5, UC-08)."""
import asyncio
import json
from unittest.mock import AsyncMock, patch
import pytest
from app.application.dtos.triage_job_dto import TriageJobPayloadDTO, TriageResultPayloadDTO
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.infrastructure.adapters.gemini_adapter import AIUnavailableException
from app.infrastructure.adapters.redis_worker import TriageJobWorker


@pytest.fixture
def sample_job_payload():
    return {
        "inspectionId": "11111111-1111-1111-1111-111111111111",
        "correlationId": "22222222-2222-2222-2222-222222222222",
        "photoObjectKey": "inspections/2026/11/photo-1.webp",
        "motorcycleModel": "MOTTU_SPORT_110I",
        "reportedContext": "RECEIVED_LIKE_THIS",
        "hoursSincePickup": 18.5,
        "kmDrivenSincePickup": 35,
        "riderNotes": "Peguei a moto hoje cedo com esse ralado no manete.",
    }


@pytest.mark.asyncio
async def test_worker_processes_valid_job_success(sample_job_payload):
    worker = TriageJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    with patch.object(worker.queue_adapter, "push_triage_result", side_effect=fake_push), \
         patch.object(worker, "_fetch_photo_bytes", return_value=b"fake_image_bytes"):

        success = await worker.process_single_job(sample_job_payload)
        assert success is True
        assert len(pushed_results) == 1

        result = pushed_results[0]
        assert result["inspectionId"] == sample_job_payload["inspectionId"]
        assert result["correlationId"] == sample_job_payload["correlationId"]
        assert result["outcome"] == "SUCCESS"
        assert result["isValidMotorcyclePhoto"] is True
        assert result["confidence"] == 0.91
        assert result["suggestedVerdict"] == "PRE_EXISTENTE"
        assert len(result["matchedParts"]) >= 1
        assert "manete" in result["technicalSummary"].lower()
        assert "0,00" in result["customerFriendlyMessage"] or "0.00" in result["customerFriendlyMessage"]


@pytest.mark.asyncio
async def test_worker_handles_gemini_unavailable(sample_job_payload):
    """When Gemini fails, worker pushes outcome: 'AI_UNAVAILABLE' to trigger AWAITING_AI_RECOVERY (UC-08)."""
    worker = TriageJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    async def fake_inspect(*args, **kwargs):
        raise AIUnavailableException("Gemini API 503 Service Unavailable")

    with patch.object(worker.queue_adapter, "push_triage_result", side_effect=fake_push), \
         patch.object(worker.vision_adapter, "inspect_damage_photo", side_effect=fake_inspect), \
         patch.object(worker, "_fetch_photo_bytes", return_value=b"fake_image_bytes"):

        success = await worker.process_single_job(sample_job_payload)
        assert success is False
        assert len(pushed_results) == 1

        result = pushed_results[0]
        assert result["inspectionId"] == sample_job_payload["inspectionId"]
        assert result["correlationId"] == sample_job_payload["correlationId"]
        assert result["outcome"] == "AI_UNAVAILABLE"
        # In fallback mode, fields like confidence/suggestedVerdict must not be set or be None
        assert result.get("suggestedVerdict") is None


@pytest.mark.asyncio
async def test_worker_crag_low_similarity_downgrade(sample_job_payload):
    """When CRAG similarity is < 0.72, confidence must be downgraded to <= 0.75."""
    worker = TriageJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    # Return candidate with low cosine similarity (e.g. 0.60)
    low_sim_candidate = PartCatalogItem(
        vtex_sku="SKU-SPT-001",
        motorcycle_model=MotorcycleModel.MOTTU_SPORT_110I,
        part_name="Manete de Freio",
        category="Comandos",
        unit_price_brl=25.0,
        embedding=[0.0] * 768,
    )

    with patch.object(worker.queue_adapter, "push_triage_result", side_effect=fake_push), \
         patch.object(worker, "_fetch_photo_bytes", return_value=b"fake_image_bytes"), \
         patch.object(worker.catalog_adapter, "find_similar_parts_with_scores", return_value=[(low_sim_candidate, 0.60)]):

        success = await worker.process_single_job(sample_job_payload)
        assert success is True
        assert len(pushed_results) == 1
        result = pushed_results[0]
        assert result["confidence"] <= 0.75


@pytest.mark.asyncio
async def test_worker_crag_ambiguous_match_downgrade(sample_job_payload):
    """When two candidates have close scores (< 0.02 delta), confidence must be downgraded to <= 0.75."""
    worker = TriageJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    c1 = PartCatalogItem(
        vtex_sku="SKU-SPT-001",
        motorcycle_model=MotorcycleModel.MOTTU_SPORT_110I,
        part_name="Manete de Freio",
        category="Comandos",
        unit_price_brl=25.0,
    )
    c2 = PartCatalogItem(
        vtex_sku="SKU-SPT-002",
        motorcycle_model=MotorcycleModel.MOTTU_SPORT_110I,
        part_name="Manete de Embreagem",
        category="Comandos",
        unit_price_brl=26.0,
    )

    with patch.object(worker.queue_adapter, "push_triage_result", side_effect=fake_push), \
         patch.object(worker, "_fetch_photo_bytes", return_value=b"fake_image_bytes"), \
         patch.object(worker.catalog_adapter, "find_similar_parts_with_scores", return_value=[(c1, 0.88), (c2, 0.87)]):

        success = await worker.process_single_job(sample_job_payload)
        assert success is True
        assert len(pushed_results) == 1
        result = pushed_results[0]
        assert result["confidence"] <= 0.75


@pytest.mark.asyncio
async def test_worker_48h_100km_exemption_rule(sample_job_payload):
    worker = TriageJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    # Over 48h -> should be MAU_USO (not pre-existing)
    sample_job_payload["hoursSincePickup"] = 72.0
    sample_job_payload["kmDrivenSincePickup"] = 150

    with patch.object(worker.queue_adapter, "push_triage_result", side_effect=fake_push), \
         patch.object(worker, "_fetch_photo_bytes", return_value=b"fake_image_bytes"):

        success = await worker.process_single_job(sample_job_payload)
        assert success is True
        assert len(pushed_results) == 1
        result = pushed_results[0]
        assert result["suggestedVerdict"] == "MAU_USO"


@pytest.mark.asyncio
async def test_worker_graceful_shutdown():
    worker = TriageJobWorker()
    worker.stop()
    assert worker.is_running is False
