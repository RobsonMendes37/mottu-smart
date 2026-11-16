"""Pure domain service for Reclame Aqui (Voice of the Customer - VoC) analytics.

Hexagonal Architecture Law 2: Standard Python only. Prohibited to import external frameworks.
"""
from typing import Any, Dict, List, Optional


def quantify_resolution_breakdown(
    complaints: List[Dict[str, Any]],
    summary_override: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    """Quantify complaints into RESOLVED, UNRESOLVED, and IN_REPLY with exact percentages."""
    if summary_override:
        total = int(summary_override.get("totalComplaints", 0))
        resolved = int(summary_override.get("resolvedCount", 0))
        unresolved = int(summary_override.get("unresolvedCount", 0))
        in_reply = int(summary_override.get("inReplyCount", 0))
    else:
        total = len(complaints)
        resolved = sum(1 for c in complaints if str(c.get("status", "")).upper() == "RESOLVED")
        unresolved = sum(1 for c in complaints if str(c.get("status", "")).upper() == "UNRESOLVED")
        in_reply = total - resolved - unresolved

    if total <= 0:
        return {
            "total_complaints": 0,
            "resolved_count": 0,
            "unresolved_count": 0,
            "in_reply_count": 0,
            "resolved_pct": 0.0,
            "unresolved_pct": 0.0,
            "in_reply_pct": 0.0,
        }

    resolved_pct = round((resolved * 100.0) / total, 1)
    unresolved_pct = round((unresolved * 100.0) / total, 1)
    in_reply_pct = round(max(0.0, 100.0 - resolved_pct - unresolved_pct), 1)

    return {
        "total_complaints": total,
        "resolved_count": resolved,
        "unresolved_count": unresolved,
        "in_reply_count": in_reply,
        "resolved_pct": resolved_pct,
        "unresolved_pct": unresolved_pct,
        "in_reply_pct": in_reply_pct,
    }


def analyze_problem_categories_and_growth(
    categories: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Rank complaint categories by quarterly growth rate and compute resolution metrics."""
    analyzed: List[Dict[str, Any]] = []
    for cat in categories:
        prev_q = int(cat.get("previousQuarterCount", 0))
        curr_q = int(cat.get("currentQuarterCount", 0))
        resolved = int(cat.get("resolvedCount", 0))
        unresolved = int(cat.get("unresolvedCount", 0))
        in_reply = int(cat.get("inReplyCount", 0))
        total = resolved + unresolved + in_reply

        if prev_q <= 0:
            growth_pct = 100.0 if curr_q > 0 else 0.0
        else:
            growth_pct = round(((curr_q - prev_q) * 100.0) / prev_q, 1)

        if total <= 0:
            res_pct = 0.0
            unres_pct = 0.0
            reply_pct = 0.0
        else:
            res_pct = round((resolved * 100.0) / total, 1)
            unres_pct = round((unresolved * 100.0) / total, 1)
            reply_pct = round(max(0.0, 100.0 - res_pct - unres_pct), 1)

        analyzed.append(
            {
                "category_key": str(cat.get("categoryKey", "OTHER")),
                "category_label": str(cat.get("categoryLabel", "Outros")),
                "total_count": total,
                "previous_quarter_count": prev_q,
                "current_quarter_count": curr_q,
                "growth_rate_pct": growth_pct,
                "resolved_count": resolved,
                "unresolved_count": unresolved,
                "in_reply_count": in_reply,
                "resolved_pct": res_pct,
                "unresolved_pct": unres_pct,
                "in_reply_pct": reply_pct,
                "addressed_by_smart_ops": bool(cat.get("addressedBySmartOps", False)),
                "smart_ops_solution": str(cat.get("smartOpsSolution", "")),
                "smart_ops_route": str(cat.get("smartOpsRoute", "/manager/analytics")),
            }
        )

    analyzed.sort(key=lambda x: x["growth_rate_pct"], reverse=True)
    return analyzed


def calculate_smart_ops_addressability(
    analyzed_categories: List[Dict[str, Any]],
) -> Dict[str, Any]:
    """Calculate the share of Reclame Aqui complaints directly addressed by Mottu Smart Ops."""
    total_analyzed = sum(int(c.get("total_count", 0)) for c in analyzed_categories)
    addressed_count = sum(
        int(c.get("total_count", 0))
        for c in analyzed_categories
        if bool(c.get("addressed_by_smart_ops", False))
    )
    if total_analyzed <= 0:
        return {
            "total_analyzed": 0,
            "addressed_count": 0,
            "addressable_share_pct": 0.0,
        }
    share_pct = round((addressed_count * 100.0) / total_analyzed, 1)
    return {
        "total_analyzed": total_analyzed,
        "addressed_count": addressed_count,
        "addressable_share_pct": share_pct,
    }


def build_reclame_aqui_dashboard_report(
    raw_snapshot: Dict[str, Any],
    used_fallback: bool = True,
) -> Dict[str, Any]:
    """Build complete consolidated executive VoC report for the Manager Dashboard and Melissa."""
    summary_override = raw_snapshot.get("summary")
    complaints = raw_snapshot.get("recentComplaints", [])
    categories_raw = raw_snapshot.get("categories", [])

    breakdown = quantify_resolution_breakdown(complaints, summary_override=summary_override)
    ranked_categories = analyze_problem_categories_and_growth(categories_raw)
    addressability = calculate_smart_ops_addressability(ranked_categories)

    if ranked_categories:
        fastest_cat = ranked_categories[0]["category_label"]
        fastest_pct = ranked_categories[0]["growth_rate_pct"]
    else:
        fastest_cat = "N/A"
        fastest_pct = 0.0

    return {
        "company_name": str(raw_snapshot.get("companyName", "Mottu")),
        "platform": str(raw_snapshot.get("platform", "Reclame Aqui")),
        "reputation_badge": str(raw_snapshot.get("reputationBadge", "OTIMO")),
        "overall_score": float(raw_snapshot.get("overallScore", 8.2)),
        "solution_rate_pct": float(raw_snapshot.get("solutionRatePct", 81.4)),
        "would_do_business_again_pct": float(raw_snapshot.get("wouldDoBusinessAgainPct", 74.8)),
        "avg_response_time_hours": float(raw_snapshot.get("avgResponseTimeHours", 18.5)),
        "total_complaints": breakdown["total_complaints"],
        "resolved_count": breakdown["resolved_count"],
        "unresolved_count": breakdown["unresolved_count"],
        "in_reply_count": breakdown["in_reply_count"],
        "resolved_pct": breakdown["resolved_pct"],
        "unresolved_pct": breakdown["unresolved_pct"],
        "in_reply_pct": breakdown["in_reply_pct"],
        "fastest_growing_category": fastest_cat,
        "fastest_growing_pct": fastest_pct,
        "addressed_complaints_count": addressability["addressed_count"],
        "addressable_share_pct": addressability["addressable_share_pct"],
        "product_rationale_headline": (
            f"{addressability['addressable_share_pct']}% das reclamações operacionais no Reclame Aqui "
            "concentram-se na previsibilidade do valor de peças, tempo de fila em galpão e disponibilidade "
            "de estoque — os 3 pilares automatizados pelo Mottu Smart Ops."
        ),
        "categories": ranked_categories,
        "recent_complaints": list(complaints),
        "used_fallback": used_fallback,
    }

