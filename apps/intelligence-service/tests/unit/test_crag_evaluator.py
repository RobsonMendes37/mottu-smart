"""Unit tests for CRAG evaluator domain logic (UC-08, UC-11)."""
import math
import pytest
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem, CRAGEvaluationResult
from app.domain.crag_evaluator import (
    compute_cosine_similarity,
    evaluate_crag_confidence,
    evaluate_catalog_candidates,
    CRAG_MIN_SIMILARITY_THRESHOLD,
    CRAG_HUMAN_TRIAGE_CONFIDENCE,
    CRAG_AMBIGUITY_DELTA,
)


def _make_part(sku: str, name: str = "Part", price: float = 50.0) -> PartCatalogItem:
    return PartCatalogItem(
        vtex_sku=sku,
        motorcycle_model=MotorcycleModel.MOTTU_SPORT_110I,
        part_name=name,
        category="Test",
        unit_price_brl=price,
    )


# =============================================================================
# 1. Cosine Similarity Tests (768-d and Edge Cases)
# =============================================================================

def test_cosine_similarity_identical_vectors():
    vec = [0.1 * i for i in range(10)]
    sim = compute_cosine_similarity(vec, vec)
    assert sim == pytest.approx(1.0, abs=1e-5)


def test_cosine_similarity_orthogonal_vectors():
    vec_a = [1.0, 0.0, 0.0]
    vec_b = [0.0, 1.0, 0.0]
    assert compute_cosine_similarity(vec_a, vec_b) == pytest.approx(0.0)


def test_cosine_similarity_opposite_vectors():
    vec_a = [1.0, 2.0, 3.0]
    vec_b = [-1.0, -2.0, -3.0]
    assert compute_cosine_similarity(vec_a, vec_b) == pytest.approx(-1.0, abs=1e-5)


def test_cosine_similarity_768_dimensions():
    # 768-d normalized vectors
    dim = 768
    val = 1.0 / math.sqrt(dim)
    vec_a = [val] * dim
    vec_b = [val] * dim
    sim = compute_cosine_similarity(vec_a, vec_b)
    assert sim == pytest.approx(1.0, abs=1e-5)

    # Half orthogonal
    vec_c = [val if i < dim // 2 else 0.0 for i in range(dim)]
    vec_d = [0.0 if i < dim // 2 else val for i in range(dim)]
    assert compute_cosine_similarity(vec_c, vec_d) == pytest.approx(0.0)


def test_cosine_similarity_zero_vectors():
    vec_zero = [0.0] * 768
    vec_ones = [1.0] * 768
    assert compute_cosine_similarity(vec_zero, vec_ones) == 0.0
    assert compute_cosine_similarity(vec_ones, vec_zero) == 0.0
    assert compute_cosine_similarity(vec_zero, vec_zero) == 0.0


def test_cosine_similarity_length_mismatch():
    vec_768 = [0.5] * 768
    vec_512 = [0.5] * 512
    assert compute_cosine_similarity(vec_768, vec_512) == 0.0


def test_cosine_similarity_empty_and_none():
    vec = [1.0, 2.0]
    assert compute_cosine_similarity(None, vec) == 0.0
    assert compute_cosine_similarity(vec, None) == 0.0
    assert compute_cosine_similarity(None, None) == 0.0
    assert compute_cosine_similarity([], vec) == 0.0
    assert compute_cosine_similarity(vec, []) == 0.0


def test_cosine_similarity_nan_and_inf_handling():
    vec_clean = [1.0, 2.0, 3.0]
    vec_nan = [1.0, float("nan"), 3.0]
    vec_inf = [1.0, float("inf"), 3.0]
    vec_neg_inf = [1.0, float("-inf"), 3.0]

    assert compute_cosine_similarity(vec_nan, vec_clean) == 0.0
    assert compute_cosine_similarity(vec_clean, vec_nan) == 0.0
    assert compute_cosine_similarity(vec_inf, vec_clean) == 0.0
    assert compute_cosine_similarity(vec_clean, vec_inf) == 0.0
    assert compute_cosine_similarity(vec_neg_inf, vec_clean) == 0.0


def test_cosine_similarity_precision_clamping():
    # Specific vectors triggering floating point jitter beyond [-1.0, 1.0]
    vec_a = [0.2769707237325105, 0.5272012376644482, -0.9836097435777491, 0.5643991309654379, 0.08485040828425339]
    vec_gt_b = [0.2769707237325107, 0.5272012376644485, -0.9836097435777482, 0.5643991309654385, 0.08485040828425336]
    vec_lt_b = [-x for x in vec_gt_b]

    # Without clamping, raw dot_product / (norm_a * norm_b) produces 1.0000000000000002 and -1.0000000000000002
    assert compute_cosine_similarity(vec_a, vec_gt_b) == 1.0
    assert compute_cosine_similarity(vec_a, vec_lt_b) == -1.0



# =============================================================================
# 2. CRAG Confidence Evaluation Tests
# =============================================================================

def test_evaluate_crag_confidence_high_similarity():
    part = _make_part("SKU-001")
    res = evaluate_crag_confidence(
        similarity_score=0.88,
        initial_confidence=0.92,
        matched_part=part,
    )
    assert res.similarity_score == 0.88
    assert res.confidence_adjusted == 0.92
    assert not res.is_hallucination_suspected
    assert not res.requires_human_triage
    assert res.matched_part == part
    assert res.triage_reason is None


def test_evaluate_crag_confidence_exact_threshold():
    res = evaluate_crag_confidence(
        similarity_score=CRAG_MIN_SIMILARITY_THRESHOLD,
        initial_confidence=0.85,
    )
    assert res.similarity_score == CRAG_MIN_SIMILARITY_THRESHOLD
    assert res.confidence_adjusted == 0.85
    assert not res.is_hallucination_suspected
    assert not res.requires_human_triage
    assert res.triage_reason is None


def test_evaluate_crag_confidence_low_similarity():
    part = _make_part("SKU-002")
    res = evaluate_crag_confidence(
        similarity_score=0.65,
        initial_confidence=0.95,
        matched_part=part,
    )
    assert res.similarity_score == 0.65
    assert res.confidence_adjusted == CRAG_HUMAN_TRIAGE_CONFIDENCE  # 0.75 (< 0.80)
    assert res.is_hallucination_suspected
    assert res.requires_human_triage
    assert res.matched_part == part
    assert res.triage_reason == "LOW_AI_CONFIDENCE"


def test_evaluate_crag_confidence_preserves_lower_initial_confidence():
    res = evaluate_crag_confidence(
        similarity_score=0.60,
        initial_confidence=0.62,
    )
    assert res.confidence_adjusted == 0.62
    assert res.is_hallucination_suspected
    assert res.requires_human_triage
    assert res.triage_reason == "LOW_AI_CONFIDENCE"


def test_evaluate_crag_confidence_ambiguous_match():
    part = _make_part("SKU-003")
    res = evaluate_crag_confidence(
        similarity_score=0.85,
        initial_confidence=0.90,
        matched_part=part,
        is_ambiguous=True,
    )
    assert res.similarity_score == 0.85
    assert res.confidence_adjusted == CRAG_HUMAN_TRIAGE_CONFIDENCE  # 0.75
    assert res.is_hallucination_suspected
    assert res.requires_human_triage
    assert res.matched_part == part
    assert res.triage_reason == "AMBIGUOUS_PART_MATCH"


# =============================================================================
# 3. Candidate Ranking & Ambiguity Detection Tests
# =============================================================================

def test_evaluate_catalog_candidates_empty_or_none():
    # Empty candidates
    res_empty = evaluate_catalog_candidates(
        query_embedding=[1.0, 0.0],
        candidates=[],
        initial_confidence=0.90,
    )
    assert res_empty.similarity_score == 0.0
    assert res_empty.confidence_adjusted == CRAG_HUMAN_TRIAGE_CONFIDENCE
    assert res_empty.is_hallucination_suspected
    assert res_empty.requires_human_triage
    assert res_empty.matched_part is None
    assert res_empty.triage_reason == "LOW_AI_CONFIDENCE"

    # None query
    res_none = evaluate_catalog_candidates(
        query_embedding=None,
        candidates=[(_make_part("SKU-1"), [1.0, 0.0])],
        initial_confidence=0.90,
    )
    assert res_none.requires_human_triage
    assert res_none.triage_reason == "LOW_AI_CONFIDENCE"


def test_evaluate_catalog_candidates_top_match_high_confidence():
    part_a = _make_part("SKU-A", "Manete de Freio")
    part_b = _make_part("SKU-B", "Pneu Traseiro")
    query = [1.0, 0.0, 0.0]
    candidates = [
        (part_a, [0.9, 0.1, 0.0]),  # similarity ~ 0.99
        (part_b, [0.1, 0.9, 0.0]),  # similarity ~ 0.11
    ]

    res = evaluate_catalog_candidates(query, candidates, initial_confidence=0.95)
    assert res.matched_part == part_a
    assert res.similarity_score > CRAG_MIN_SIMILARITY_THRESHOLD
    assert res.confidence_adjusted == 0.95
    assert not res.requires_human_triage
    assert res.triage_reason is None


def test_evaluate_catalog_candidates_ambiguous_close_scores():
    part_a = _make_part("SKU-A", "Espelho Retrovisor Direito")
    part_b = _make_part("SKU-B", "Espelho Retrovisor Esquerdo")
    query = [1.0, 1.0, 0.0]
    # Two candidates with nearly identical similarity (diff < 0.02)
    candidates = [
        (part_a, [1.0, 0.99, 0.0]),
        (part_b, [0.99, 1.0, 0.0]),
    ]

    res = evaluate_catalog_candidates(
        query, candidates, initial_confidence=0.90, ambiguity_delta=CRAG_AMBIGUITY_DELTA
    )
    assert res.similarity_score >= CRAG_MIN_SIMILARITY_THRESHOLD
    assert res.confidence_adjusted == CRAG_HUMAN_TRIAGE_CONFIDENCE  # 0.75
    assert res.is_hallucination_suspected
    assert res.requires_human_triage
    assert res.triage_reason == "AMBIGUOUS_PART_MATCH"


def test_evaluate_catalog_candidates_same_sku_not_ambiguous():
    part_a1 = _make_part("SKU-A", "Lona de Freio")
    part_a2 = _make_part("SKU-A", "Lona de Freio (Duplicado)")
    query = [1.0, 0.0]
    candidates = [
        (part_a1, [1.0, 0.0]),
        (part_a2, [0.999, 0.01]),
    ]

    res = evaluate_catalog_candidates(query, candidates, initial_confidence=0.92)
    # Since SKUs are identical, this should NOT trigger AMBIGUOUS_PART_MATCH
    assert not res.requires_human_triage
    assert res.confidence_adjusted == 0.92
    assert res.triage_reason is None


def test_evaluate_catalog_candidates_below_threshold_not_ambiguous():
    part_a = _make_part("SKU-A")
    part_b = _make_part("SKU-B")
    query = [1.0, 0.0]
    # Both low similarity < 0.72
    candidates = [
        (part_a, [0.5, 0.8]),
        (part_b, [0.5, 0.81]),
    ]
    res = evaluate_catalog_candidates(query, candidates, initial_confidence=0.90)
    assert res.similarity_score < CRAG_MIN_SIMILARITY_THRESHOLD
    assert res.requires_human_triage
    assert res.triage_reason == "LOW_AI_CONFIDENCE"
