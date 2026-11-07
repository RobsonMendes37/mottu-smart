"""Unit tests covering 100% of app/domain (enums, models, normalizer, evaluator)."""
import pytest
from app.domain.enums import (
    MotorcycleModel,
    DamageVerdict,
    SafetyRiskLevel,
    ReportedOriginContext,
    QueuePriorityReason,
    InspectionStatus,
    TriageOutcome,
)
from app.domain.models import PartCatalogItem, DamageItem, CRAGEvaluationResult
from app.domain.vtex_normalizer import is_consumable_part, normalize_vtex_item
from app.domain.crag_evaluator import compute_cosine_similarity, evaluate_crag_confidence


# -----------------------------------------------------------------------------
# Enums Tests
# -----------------------------------------------------------------------------
def test_motorcycle_model_values():
    assert MotorcycleModel.MOTTU_SPORT_110I.value == "MOTTU_SPORT_110I"
    assert MotorcycleModel.HONDA_POP_110I.value == "HONDA_POP_110I"
    assert MotorcycleModel.TVS_APACHE_160.value == "TVS_APACHE_160"
    assert MotorcycleModel.TVS_RAIDER_125.value == "TVS_RAIDER_125"


def test_damage_verdict_values():
    assert DamageVerdict.PRE_EXISTENTE.value == "PRE_EXISTENTE"
    assert DamageVerdict.DESGASTE_NATURAL.value == "DESGASTE_NATURAL"
    assert DamageVerdict.MAU_USO.value == "MAU_USO"


def test_safety_risk_level_values():
    assert SafetyRiskLevel.SAFE_TO_RIDE.value == "SAFE_TO_RIDE"
    assert SafetyRiskLevel.CRITICAL_STOPPED.value == "CRITICAL_STOPPED"


def test_reported_origin_context_values():
    assert ReportedOriginContext.RECEIVED_LIKE_THIS.value == "RECEIVED_LIKE_THIS"
    assert ReportedOriginContext.NORMAL_USE_WEAR.value == "NORMAL_USE_WEAR"
    assert ReportedOriginContext.ACCIDENT_OR_DROP.value == "ACCIDENT_OR_DROP"


def test_queue_priority_reason_values():
    assert QueuePriorityReason.CRITICAL_SAFETY.value == "CRITICAL_SAFETY"
    assert QueuePriorityReason.RIDER_CONTESTATION.value == "RIDER_CONTESTATION"
    assert QueuePriorityReason.LOW_AI_CONFIDENCE.value == "LOW_AI_CONFIDENCE"
    assert QueuePriorityReason.FRAUD_SUSPICION.value == "FRAUD_SUSPICION"


def test_inspection_status_values():
    assert InspectionStatus.QUEUED_AI.value == "QUEUED_AI"
    assert InspectionStatus.INVALID_IMAGE.value == "INVALID_IMAGE"
    assert InspectionStatus.AWAITING_AI_RECOVERY.value == "AWAITING_AI_RECOVERY"
    assert InspectionStatus.AUTO_RESOLVED.value == "AUTO_RESOLVED"
    assert InspectionStatus.HUMAN_TRIAGE.value == "HUMAN_TRIAGE"
    assert InspectionStatus.HUMAN_RESOLVED.value == "HUMAN_RESOLVED"
    assert InspectionStatus.SCHEDULED.value == "SCHEDULED"
    assert InspectionStatus.CLOSED.value == "CLOSED"


def test_triage_outcome_values():
    assert TriageOutcome.SUCCESS.value == "SUCCESS"
    assert TriageOutcome.AI_UNAVAILABLE.value == "AI_UNAVAILABLE"


# -----------------------------------------------------------------------------
# Domain Models Tests
# -----------------------------------------------------------------------------
def test_part_catalog_item_creation():
    item = PartCatalogItem(
        vtex_sku="SKU-100",
        motorcycle_model=MotorcycleModel.MOTTU_SPORT_110I,
        part_name="Manete de Freio",
        category="Comandos",
        unit_price_brl=25.00,
        is_natural_wear=False,
        price_overridden=True,
        image_url="http://image.url/1.png",
        embedding=[0.1, 0.2, 0.3],
    )
    assert item.vtex_sku == "SKU-100"
    assert item.motorcycle_model == MotorcycleModel.MOTTU_SPORT_110I
    assert item.part_name == "Manete de Freio"
    assert item.category == "Comandos"
    assert item.unit_price_brl == 25.00
    assert not item.is_natural_wear
    assert item.price_overridden
    assert item.image_url == "http://image.url/1.png"
    assert item.embedding == [0.1, 0.2, 0.3]


def test_part_catalog_item_negative_price_raises_error():
    with pytest.raises(ValueError, match="unit_price_brl cannot be negative"):
        PartCatalogItem(
            vtex_sku="SKU-ERR",
            motorcycle_model=MotorcycleModel.HONDA_POP_110I,
            part_name="Invalido",
            category="Teste",
            unit_price_brl=-5.0,
        )


def test_damage_item_creation():
    damage = DamageItem(
        part_name="Espelho Direito",
        unit_price_brl=42.50,
        catalog_part_id="uuid-1234",
        is_natural_wear=False,
    )
    assert damage.part_name == "Espelho Direito"
    assert damage.unit_price_brl == 42.50
    assert damage.catalog_part_id == "uuid-1234"
    assert not damage.is_natural_wear


def test_crag_evaluation_result_creation():
    res = CRAGEvaluationResult(
        similarity_score=0.85,
        confidence_adjusted=0.85,
        is_hallucination_suspected=False,
        requires_human_triage=False,
    )
    assert res.similarity_score == 0.85
    assert res.confidence_adjusted == 0.85
    assert not res.is_hallucination_suspected
    assert not res.requires_human_triage
    assert res.matched_part is None


# -----------------------------------------------------------------------------
# VTEX Normalizer Tests
# -----------------------------------------------------------------------------
def test_is_consumable_part():
    assert is_consumable_part("Lona de Freio Traseira")
    assert is_consumable_part("Pastilha de Freio Dianteira")
    assert is_consumable_part("Pneu Traseiro 90/90-18")
    assert is_consumable_part("Óleo 10W30 Mineral")
    assert is_consumable_part("Oleo 4T")
    assert is_consumable_part("Filtro de Ar")
    assert is_consumable_part("Vela de Ignição")
    assert is_consumable_part("Kit Corrente Transmissão")
    assert is_consumable_part("Kit Relação Completa")
    assert is_consumable_part("Kit Relacao Completa")
    assert not is_consumable_part("Manete de Freio")
    assert not is_consumable_part("Espelho Retrovisor")
    assert not is_consumable_part("Carenagem Lateral")


def test_normalize_vtex_item():
    raw = {
        "sku": "VTEX-001",
        "productName": "Lona de Freio Traseira Sport",
        "category": "Freios",
        "price": 33.50,
        "imageUrl": "https://mottu.com.br/lona.png",
    }
    item = normalize_vtex_item(raw, MotorcycleModel.MOTTU_SPORT_110I)
    assert item.vtex_sku == "VTEX-001"
    assert item.motorcycle_model == MotorcycleModel.MOTTU_SPORT_110I
    assert item.part_name == "Lona de Freio Traseira Sport"
    assert item.category == "Freios"
    assert item.unit_price_brl == 33.50
    assert item.is_natural_wear is True
    assert item.price_overridden is False
    assert item.image_url == "https://mottu.com.br/lona.png"


def test_normalize_vtex_item_with_defaults():
    raw = {}
    item = normalize_vtex_item(raw, MotorcycleModel.HONDA_POP_110I)
    assert item.vtex_sku == ""
    assert item.motorcycle_model == MotorcycleModel.HONDA_POP_110I
    assert item.part_name == ""
    assert item.category == "General"
    assert item.unit_price_brl == 0.0
    assert item.is_natural_wear is False
    assert item.price_overridden is False
    assert item.image_url is None


# -----------------------------------------------------------------------------
# CRAG Evaluator Tests
# -----------------------------------------------------------------------------
def test_compute_cosine_similarity():
    vec_a = [1.0, 0.0, 0.0]
    vec_b = [1.0, 0.0, 0.0]
    assert compute_cosine_similarity(vec_a, vec_b) == pytest.approx(1.0)

    vec_c = [0.0, 1.0, 0.0]
    assert compute_cosine_similarity(vec_a, vec_c) == pytest.approx(0.0)

    # Empty / mismatched length
    assert compute_cosine_similarity([], [1.0]) == 0.0
    assert compute_cosine_similarity([1.0], []) == 0.0
    assert compute_cosine_similarity([1.0, 2.0], [1.0]) == 0.0

    # Zero vector norm
    assert compute_cosine_similarity([0.0, 0.0], [1.0, 1.0]) == 0.0
    assert compute_cosine_similarity([1.0, 1.0], [0.0, 0.0]) == 0.0


def test_evaluate_crag_confidence_high_similarity():
    part = PartCatalogItem(
        vtex_sku="SKU-1",
        motorcycle_model=MotorcycleModel.TVS_APACHE_160,
        part_name="Manete",
        category="Comandos",
        unit_price_brl=30.0,
    )
    result = evaluate_crag_confidence(similarity_score=0.88, initial_confidence=0.92, matched_part=part)
    assert result.similarity_score == 0.88
    assert result.confidence_adjusted == 0.92
    assert result.is_hallucination_suspected is False
    assert result.requires_human_triage is False
    assert result.matched_part == part


def test_evaluate_crag_confidence_low_similarity():
    result = evaluate_crag_confidence(similarity_score=0.65, initial_confidence=0.90)
    assert result.similarity_score == 0.65
    assert result.confidence_adjusted == 0.75  # capped to 0.75 (< 0.80)
    assert result.is_hallucination_suspected is True
    assert result.requires_human_triage is True
    assert result.matched_part is None

    # Even lower initial confidence is kept
    result_lower = evaluate_crag_confidence(similarity_score=0.50, initial_confidence=0.60)
    assert result_lower.confidence_adjusted == 0.60
    assert result_lower.is_hallucination_suspected is True
    assert result_lower.requires_human_triage is True
