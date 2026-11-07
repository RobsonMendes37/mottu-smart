"""Pure Python domain module for accident video parts deduplication (UC-15)."""
from typing import Dict, List, Tuple
from app.domain.models import VideoPartDetection, DeduplicatedPart, AccidentEvaluationResult


def _normalize_key(part_id: str | None, name: str) -> str:
    if part_id and part_id.strip():
        return part_id.strip()
    return name.strip().lower()


def deduplicate_detected_parts(detections: List[VideoPartDetection]) -> List[DeduplicatedPart]:
    """
    Deduplicates temporal detections of parts seen in an accident video.
    If the same part appears at multiple seconds, keeps a single replacement unit
    with the earliest video timestamp.
    """
    if not detections:
        return []

    # Map key -> (best_detection, min_timestamp)
    grouped: Dict[str, Tuple[VideoPartDetection, int | None]] = {}

    for det in detections:
        key = _normalize_key(det.catalog_part_id, det.part_name)
        ts = det.video_timestamp_seconds

        if key not in grouped:
            grouped[key] = (det, ts)
        else:
            existing_det, existing_ts = grouped[key]
            # Select earliest timestamp
            if existing_ts is None:
                new_ts = ts
            elif ts is None:
                new_ts = existing_ts
            else:
                new_ts = min(existing_ts, ts)
            grouped[key] = (existing_det, new_ts)

    deduplicated: List[DeduplicatedPart] = []
    for det, min_ts in grouped.values():
        deduplicated.append(
            DeduplicatedPart(
                catalog_part_id=det.catalog_part_id,
                part_name=det.part_name,
                unit_price_brl=det.unit_price_brl,
                quantity=1,
                video_timestamp_seconds=min_ts,
            )
        )

    return deduplicated


def evaluate_accident_video_detections(
    detections: List[VideoPartDetection],
    raw_confidence: float,
    raw_summary: str,
    structural_risk: bool,
) -> AccidentEvaluationResult:
    """Consolidates video detections into an evaluation result with totals."""
    deduplicated = deduplicate_detected_parts(detections)
    total_parts_count = sum(p.quantity for p in deduplicated)
    subtotal_parts_brl = sum(p.unit_price_brl * p.quantity for p in deduplicated)

    return AccidentEvaluationResult(
        confidence=raw_confidence,
        structural_risk_alert=structural_risk,
        ai_technical_summary=raw_summary,
        deduplicated_parts=deduplicated,
        total_parts_count=total_parts_count,
        subtotal_parts_brl=round(subtotal_parts_brl, 2),
    )

