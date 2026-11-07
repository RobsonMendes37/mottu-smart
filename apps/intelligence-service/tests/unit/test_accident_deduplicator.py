"""Unit tests for pure Python accident video deduplication (UC-15)."""
import pytest
from app.domain.enums import VehicleDrivability, AccidentReportStatus
from app.domain.models import VideoPartDetection, DeduplicatedPart, AccidentEvaluationResult
from app.domain.accident_deduplicator import (
    deduplicate_detected_parts,
    evaluate_accident_video_detections,
)


def test_deduplicate_empty_detections():
    """UC-15.1: Lista vazia deve retornar lista vazia de peças deduplicadas."""
    result = deduplicate_detected_parts([])
    assert result == []


def test_deduplicate_duplicate_sightings_of_same_part():
    """UC-15.2: Mesma peça avistada em múltiplos timestamps (ex: ts=4 e ts=22) deve ser consolidada em 1 unidade com o primeiro timestamp."""
    d1 = VideoPartDetection(
        part_name="Manete de Freio Direito",
        unit_price_brl=25.0,
        catalog_part_id="part-uuid-1",
        video_timestamp_seconds=22,
        confidence=0.88,
    )
    d2 = VideoPartDetection(
        part_name="Manete de Freio Direito",
        unit_price_brl=25.0,
        catalog_part_id="part-uuid-1",
        video_timestamp_seconds=4,
        confidence=0.95,
    )

    result = deduplicate_detected_parts([d1, d2])

    assert len(result) == 1
    item = result[0]
    assert item.catalog_part_id == "part-uuid-1"
    assert item.part_name == "Manete de Freio Direito"
    assert item.unit_price_brl == 25.0
    assert item.quantity == 1
    assert item.video_timestamp_seconds == 4  # Menor timestamp (primeiro avistamento)


def test_deduplicate_distinct_parts():
    """UC-15.3: Peças distintas devem ser preservadas sem mescla indevida."""
    p1 = VideoPartDetection(
        part_name="Manete de Freio",
        unit_price_brl=25.0,
        catalog_part_id="uuid-1",
        video_timestamp_seconds=6,
    )
    p2 = VideoPartDetection(
        part_name="Retrovisor Direito",
        unit_price_brl=42.50,
        catalog_part_id="uuid-2",
        video_timestamp_seconds=14,
    )
    p3 = VideoPartDetection(
        part_name="Pedal de Apoio",
        unit_price_brl=80.0,
        catalog_part_id="uuid-3",
        video_timestamp_seconds=21,
    )

    result = deduplicate_detected_parts([p1, p2, p3])

    assert len(result) == 3
    names = [r.part_name for r in result]
    assert "Manete de Freio" in names
    assert "Retrovisor Direito" in names
    assert "Pedal de Apoio" in names


def test_deduplicate_by_normalized_name_when_id_none():
    """UC-15.4: Quando catalog_part_id for None, deve agrupar por nome normalizado."""
    p1 = VideoPartDetection(
        part_name="Aba do Tanque Direita",
        unit_price_brl=95.0,
        catalog_part_id=None,
        video_timestamp_seconds=8,
    )
    p2 = VideoPartDetection(
        part_name="aba do tanque direita",
        unit_price_brl=95.0,
        catalog_part_id=None,
        video_timestamp_seconds=18,
    )

    result = deduplicate_detected_parts([p1, p2])
    assert len(result) == 1
    assert result[0].part_name == "Aba do Tanque Direita"
    assert result[0].video_timestamp_seconds == 8
    assert result[0].quantity == 1


def test_evaluate_accident_video_detections_full():
    """UC-15.5: Avaliação completa calculando subtotal, contagem e limites de confiança."""
    p1 = VideoPartDetection(
        part_name="Manete",
        unit_price_brl=25.0,
        catalog_part_id="uuid-1",
        video_timestamp_seconds=5,
    )
    p2 = VideoPartDetection(
        part_name="Manete",
        unit_price_brl=25.0,
        catalog_part_id="uuid-1",
        video_timestamp_seconds=15,
    )
    p3 = VideoPartDetection(
        part_name="Pedal",
        unit_price_brl=80.0,
        catalog_part_id="uuid-2",
        video_timestamp_seconds=10,
    )

    eval_result = evaluate_accident_video_detections(
        detections=[p1, p2, p3],
        raw_confidence=0.94,
        raw_summary="Queda lateral com quebra de manete e pedal.",
        structural_risk=True,
    )

    assert isinstance(eval_result, AccidentEvaluationResult)
    assert eval_result.confidence == 0.94
    assert eval_result.structural_risk_alert is True
    assert eval_result.total_parts_count == 2
    assert eval_result.subtotal_parts_brl == 105.0
    assert len(eval_result.deduplicated_parts) == 2
    assert "Queda lateral" in eval_result.ai_technical_summary


def test_deduplicate_with_none_timestamps():
    """UC-15.8: Cobertura de ramos onde timestamps são None."""
    # p1 com ts=None e p2 com ts=12
    p1 = VideoPartDetection(part_name="Farol", unit_price_brl=150.0, catalog_part_id="id-farol", video_timestamp_seconds=None)
    p2 = VideoPartDetection(part_name="Farol", unit_price_brl=150.0, catalog_part_id="id-farol", video_timestamp_seconds=12)
    res1 = deduplicate_detected_parts([p1, p2])
    assert res1[0].video_timestamp_seconds == 12

    # p3 com ts=15 e p4 com ts=None
    p3 = VideoPartDetection(part_name="Lanterna", unit_price_brl=80.0, catalog_part_id="id-lanterna", video_timestamp_seconds=15)
    p4 = VideoPartDetection(part_name="Lanterna", unit_price_brl=80.0, catalog_part_id="id-lanterna", video_timestamp_seconds=None)
    res2 = deduplicate_detected_parts([p3, p4])
    assert res2[0].video_timestamp_seconds == 15


def test_validation_video_part_detection():
    """UC-15.6: Validação de preços negativos em VideoPartDetection."""
    with pytest.raises(ValueError, match="unit_price_brl cannot be negative"):
        VideoPartDetection(
            part_name="Teste",
            unit_price_brl=-10.0,
            catalog_part_id="id",
            video_timestamp_seconds=5,
        )
    """UC-15.6: Validação de preços negativos em VideoPartDetection."""
    with pytest.raises(ValueError, match="unit_price_brl cannot be negative"):
        VideoPartDetection(
            part_name="Teste",
            unit_price_brl=-10.0,
            catalog_part_id="id",
            video_timestamp_seconds=5,
        )


def test_enums_accident_available():
    """UC-15.7: Enums VehicleDrivability e AccidentReportStatus devem ter valores padronizados."""
    assert VehicleDrivability.DRIVABLE == "DRIVABLE"
    assert VehicleDrivability.NEEDS_TOWING == "NEEDS_TOWING"
    assert AccidentReportStatus.QUEUED_AI == "QUEUED_AI"
    assert AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP == "ESTIMATED_PENDING_WORKSHOP"
    assert AccidentReportStatus.WORKSHOP_CONFIRMED == "WORKSHOP_CONFIRMED"
    assert AccidentReportStatus.AWAITING_AI_RECOVERY == "AWAITING_AI_RECOVERY"
