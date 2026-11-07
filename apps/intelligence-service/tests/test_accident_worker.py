"""Integration and unit tests for the Accident Video AI Worker (docs/02 §6.5, UC-15)."""
import asyncio
import json
from unittest.mock import AsyncMock, patch
import pytest
from app.application.dtos.accident_job_dto import AccidentJobPayloadDTO, AccidentResultPayloadDTO
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.infrastructure.adapters.accident_worker import AccidentJobWorker, AccidentRedisQueueAdapter
from app.infrastructure.adapters.gemini_adapter import AIUnavailableException
from app.infrastructure.adapters.gemini_video_adapter import GeminiVideoAdapter


@pytest.fixture
def sample_accident_job():
    return {
        "accidentReportId": "33333333-3333-3333-3333-333333333333",
        "correlationId": "44444444-4444-4444-4444-444444444444",
        "videoObjectKey": "accidents/2026/11/crash_video.mp4",
        "photoObjectKeys": ["accidents/2026/11/crash_photo1.webp"],
        "motorcycleModel": "MOTTU_SPORT_110I",
        "drivability": "NEEDS_TOWING",
        "riderNotes": "Queda lateral após óleo na pista na rotatória.",
    }


@pytest.mark.asyncio
async def test_accident_worker_processes_valid_job_success(sample_accident_job):
    """Verifies that the worker processes the video, deduplicates recurring parts, and emits SUCCESS."""
    worker = AccidentJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    with patch.object(worker.queue_adapter, "push_accident_result", side_effect=fake_push), \
         patch.object(worker, "_fetch_video_bytes", return_value=b"fake_mp4_bytes"):

        success = await worker.process_single_job(sample_accident_job)
        assert success is True
        assert len(pushed_results) == 1

        result = pushed_results[0]
        assert result["accidentReportId"] == sample_accident_job["accidentReportId"]
        assert result["correlationId"] == sample_accident_job["correlationId"]
        assert result["outcome"] == "SUCCESS"
        assert result["confidence"] == 0.94
        assert result["structuralRiskAlert"] is False
        assert "lateral direita" in result["aiTechnicalSummary"].lower()

        # Deduplication check:
        # Mock detects Manete at t=6 and t=28. Earliest (6) must be preserved and quantity = 1.
        dedup_parts = result["deduplicatedParts"]
        assert len(dedup_parts) == 3

        part_names = [p["partName"] for p in dedup_parts]
        assert "Manete de Freio Direito" in part_names
        assert "Retrovisor Direito" in part_names
        assert "Pedal de Apoio Dianteiro Direito" in part_names

        manete_item = next(p for p in dedup_parts if p["partName"] == "Manete de Freio Direito")
        assert manete_item["videoTimestampSeconds"] == 6
        assert manete_item["quantity"] == 1


@pytest.mark.asyncio
async def test_accident_worker_handles_gemini_unavailable(sample_accident_job):
    """When Gemini video analysis fails, worker pushes outcome: 'AI_UNAVAILABLE' to trigger AWAITING_AI_RECOVERY (UC-15)."""
    worker = AccidentJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    async def fake_inspect(*args, **kwargs):
        raise AIUnavailableException("Gemini Video 503 Overloaded")

    with patch.object(worker.queue_adapter, "push_accident_result", side_effect=fake_push), \
         patch.object(worker.video_adapter, "inspect_accident_video", side_effect=fake_inspect), \
         patch.object(worker, "_fetch_video_bytes", return_value=b"fake_mp4_bytes"):

        success = await worker.process_single_job(sample_accident_job)
        assert success is False
        assert len(pushed_results) == 1

        result = pushed_results[0]
        assert result["accidentReportId"] == sample_accident_job["accidentReportId"]
        assert result["correlationId"] == sample_accident_job["correlationId"]
        assert result["outcome"] == "AI_UNAVAILABLE"


@pytest.mark.asyncio
async def test_accident_worker_discards_invalid_job():
    """Worker cleanly returns False and discards invalid payloads without pushing to results queue."""
    worker = AccidentJobWorker()
    pushed_results = []

    async def fake_push(result):
        pushed_results.append(result)

    with patch.object(worker.queue_adapter, "push_accident_result", side_effect=fake_push):
        success = await worker.process_single_job({"invalid": "payload"})
        assert success is False
        assert len(pushed_results) == 0


@pytest.mark.asyncio
async def test_accident_worker_lifecycle_and_video_fetch():
    """Tests worker start, stop and _fetch_video_bytes fallback."""
    worker = AccidentJobWorker()
    assert worker.is_running is False
    worker.start()
    assert worker.is_running is True
    worker.stop()
    assert worker.is_running is False

    # Video fetch fallback
    data = await worker._fetch_video_bytes("non_existent_key.mp4")
    assert isinstance(data, bytes)
    assert len(data) > 0


@pytest.mark.asyncio
async def test_accident_redis_queue_adapter_graceful():
    """Tests Redis adapter resilience when Redis is offline."""
    adapter = AccidentRedisQueueAdapter()
    adapter.host = "127.0.0.1"
    adapter.port = 63799  # unreachable port

    job = await adapter.pop_accident_job(timeout_seconds=1)
    assert job is None

    # Push should not raise exception even when unreachable
    await adapter.push_accident_result({"test": "data"})


@pytest.mark.asyncio
async def test_gemini_video_adapter_mock_mode():
    """Tests GeminiVideoAdapter in mock mode (Law 8)."""
    adapter = GeminiVideoAdapter()
    result = await adapter.inspect_accident_video(b"dummy_video", "Model: MOTTU_SPORT_110I")
    assert result["isValidVideo"] is True
    assert result["confidence"] == 0.94
    assert len(result["detectedParts"]) == 4

    emb = await adapter.generate_embedding("Manete de Freio")
    assert len(emb) == 768
