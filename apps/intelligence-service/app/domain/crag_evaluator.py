"""Pure Python CRAG (Corrective Retrieval-Augmented Generation) evaluator."""
import math
from typing import List, Optional, Tuple
from app.domain.models import CRAGEvaluationResult, PartCatalogItem

CRAG_MIN_SIMILARITY_THRESHOLD: float = 0.72
CRAG_HUMAN_TRIAGE_CONFIDENCE: float = 0.75
CRAG_AMBIGUITY_DELTA: float = 0.02


def compute_cosine_similarity(
    vec_a: Optional[List[float]],
    vec_b: Optional[List[float]],
) -> float:
    """Computes cosine similarity between two numeric vectors using pure standard Python math.

    Returns 0.0 on zero-vectors, length mismatches, empty/None vectors, or non-finite numbers.
    Clamps result within [-1.0, 1.0] to prevent floating point precision overflow.
    """
    if vec_a is None or vec_b is None:
        return 0.0
    if not vec_a or not vec_b or len(vec_a) != len(vec_b):
        return 0.0

    dot_product = 0.0
    sum_sq_a = 0.0
    sum_sq_b = 0.0

    for a, b in zip(vec_a, vec_b):
        if math.isnan(a) or math.isnan(b) or math.isinf(a) or math.isinf(b):
            return 0.0
        dot_product += a * b
        sum_sq_a += a * a
        sum_sq_b += b * b

    if sum_sq_a <= 0.0 or sum_sq_b <= 0.0:
        return 0.0

    norm_a = math.sqrt(sum_sq_a)
    norm_b = math.sqrt(sum_sq_b)
    similarity = dot_product / (norm_a * norm_b)

    # Precision clamping
    if similarity > 1.0:
        return 1.0
    elif similarity < -1.0:
        return -1.0
    return round(similarity, 6)


def evaluate_crag_confidence(
    similarity_score: float,
    initial_confidence: float,
    matched_part: Optional[PartCatalogItem] = None,
    is_ambiguous: bool = False,
) -> CRAGEvaluationResult:
    """Evaluates CRAG retrieval similarity score and adjusts AI confidence score.

    Degrades confidence to min(initial_confidence, 0.75) if similarity is below threshold
    or if candidate matching is ambiguous, flagging for human triage.
    """
    if similarity_score < CRAG_MIN_SIMILARITY_THRESHOLD:
        return CRAGEvaluationResult(
            similarity_score=similarity_score,
            confidence_adjusted=min(initial_confidence, CRAG_HUMAN_TRIAGE_CONFIDENCE),
            is_hallucination_suspected=True,
            requires_human_triage=True,
            matched_part=matched_part,
            triage_reason="LOW_AI_CONFIDENCE",
        )
    elif is_ambiguous:
        return CRAGEvaluationResult(
            similarity_score=similarity_score,
            confidence_adjusted=min(initial_confidence, CRAG_HUMAN_TRIAGE_CONFIDENCE),
            is_hallucination_suspected=True,
            requires_human_triage=True,
            matched_part=matched_part,
            triage_reason="AMBIGUOUS_PART_MATCH",
        )
    return CRAGEvaluationResult(
        similarity_score=similarity_score,
        confidence_adjusted=initial_confidence,
        is_hallucination_suspected=False,
        requires_human_triage=False,
        matched_part=matched_part,
        triage_reason=None,
    )


def evaluate_catalog_candidates(
    query_embedding: Optional[List[float]],
    candidates: List[Tuple[PartCatalogItem, List[float]]],
    initial_confidence: float,
    ambiguity_delta: float = CRAG_AMBIGUITY_DELTA,
) -> CRAGEvaluationResult:
    """Ranks catalog candidates by cosine similarity against query embedding and applies CRAG evaluation.

    Detects ambiguity when top two candidates have similarity difference < ambiguity_delta.
    """
    if not query_embedding or not candidates:
        return CRAGEvaluationResult(
            similarity_score=0.0,
            confidence_adjusted=min(initial_confidence, CRAG_HUMAN_TRIAGE_CONFIDENCE),
            is_hallucination_suspected=True,
            requires_human_triage=True,
            matched_part=None,
            triage_reason="LOW_AI_CONFIDENCE",
        )

    scored_candidates: List[Tuple[PartCatalogItem, float]] = []
    for item, emb in candidates:
        sim = compute_cosine_similarity(query_embedding, emb)
        scored_candidates.append((item, sim))

    scored_candidates.sort(key=lambda x: x[1], reverse=True)
    top_item, top_score = scored_candidates[0]

    is_ambiguous = False
    if len(scored_candidates) > 1 and top_score >= CRAG_MIN_SIMILARITY_THRESHOLD:
        second_item, second_score = scored_candidates[1]
        if (top_score - second_score) < ambiguity_delta and top_item.vtex_sku != second_item.vtex_sku:
            is_ambiguous = True

    return evaluate_crag_confidence(
        similarity_score=top_score,
        initial_confidence=initial_confidence,
        matched_part=top_item,
        is_ambiguous=is_ambiguous,
    )
