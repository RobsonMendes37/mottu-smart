"""Unit tests for Reclame Aqui (VoC) pure domain analytics service (TASK-35 TDD)."""
from app.domain.services.reclame_aqui_analytics import (
    analyze_problem_categories_and_growth,
    build_reclame_aqui_dashboard_report,
    calculate_smart_ops_addressability,
    quantify_resolution_breakdown,
)


def test_quantify_resolution_breakdown_calculates_exact_counts_and_percentages():
    complaints = [
        {"status": "RESOLVED"},
        {"status": "RESOLVED"},
        {"status": "RESOLVED"},
        {"status": "UNRESOLVED"},
        {"status": "IN_REPLY"},
    ]
    result = quantify_resolution_breakdown(complaints)
    assert result["total_complaints"] == 5
    assert result["resolved_count"] == 3
    assert result["unresolved_count"] == 1
    assert result["in_reply_count"] == 1
    assert result["resolved_pct"] == 60.0
    assert result["unresolved_pct"] == 20.0
    assert result["in_reply_pct"] == 20.0


def test_quantify_resolution_breakdown_handles_empty_list():
    result = quantify_resolution_breakdown([])
    assert result["total_complaints"] == 0
    assert result["resolved_count"] == 0
    assert result["unresolved_count"] == 0
    assert result["in_reply_count"] == 0
    assert result["resolved_pct"] == 0.0
    assert result["unresolved_pct"] == 0.0
    assert result["in_reply_pct"] == 0.0


def test_quantify_resolution_breakdown_with_summary_override():
    summary = {
        "totalComplaints": 640,
        "resolvedCount": 490,
        "unresolvedCount": 100,
        "inReplyCount": 50,
    }
    result = quantify_resolution_breakdown([], summary_override=summary)
    assert result["total_complaints"] == 640
    assert result["resolved_count"] == 490
    assert result["unresolved_count"] == 100
    assert result["in_reply_count"] == 50
    assert result["resolved_pct"] == 76.6
    assert result["unresolved_pct"] == 15.6
    assert result["in_reply_pct"] == 7.8


def test_analyze_problem_categories_and_growth_sorts_by_growth_and_computes_rates():
    categories = [
        {
            "categoryKey": "FILA_ESPERA_GALPAO",
            "categoryLabel": "Tempo de Espera e Fila no Galpão",
            "previousQuarterCount": 60,
            "currentQuarterCount": 84,
            "resolvedCount": 112,
            "unresolvedCount": 22,
            "inReplyCount": 10,
            "addressedBySmartOps": True,
            "smartOpsSolution": "Triagem Autônoma por Foto (78,5% sem fila)",
            "smartOpsRoute": "/mechanic/queue",
        },
        {
            "categoryKey": "ORCAMENTO_PECAS_AVARIAS",
            "categoryLabel": "Previsibilidade no Valor e Quantidade de Peças",
            "previousQuarterCount": 78,
            "currentQuarterCount": 104,
            "resolvedCount": 142,
            "unresolvedCount": 26,
            "inReplyCount": 14,
            "addressedBySmartOps": True,
            "smartOpsSolution": "Orçamento Transparente Loja Mottu na T02/T04",
            "smartOpsRoute": "/manager/catalog",
        },
        {
            "categoryKey": "ZERO_PREV_CATEGORY",
            "categoryLabel": "Nova Categoria",
            "previousQuarterCount": 0,
            "currentQuarterCount": 10,
            "resolvedCount": 8,
            "unresolvedCount": 2,
            "inReplyCount": 0,
            "addressedBySmartOps": False,
            "smartOpsSolution": "Fluxo de Suporte",
            "smartOpsRoute": "/manager/approvals",
        },
    ]

    ranked = analyze_problem_categories_and_growth(categories)
    assert len(ranked) == 3
    # ZERO_PREV_CATEGORY has 100.0% growth when previous is 0 and current > 0
    assert ranked[0]["category_key"] == "ZERO_PREV_CATEGORY"
    assert ranked[0]["growth_rate_pct"] == 100.0
    assert ranked[1]["category_key"] == "FILA_ESPERA_GALPAO"
    assert ranked[1]["growth_rate_pct"] == 40.0
    assert ranked[2]["category_key"] == "ORCAMENTO_PECAS_AVARIAS"
    assert ranked[2]["growth_rate_pct"] == 33.3


def test_analyze_problem_categories_zero_current_and_zero_previous():
    ranked = analyze_problem_categories_and_growth(
        [
            {
                "categoryKey": "IDLE",
                "categoryLabel": "Sem Ocorrências",
                "previousQuarterCount": 0,
                "currentQuarterCount": 0,
                "resolvedCount": 0,
                "unresolvedCount": 0,
                "inReplyCount": 0,
            }
        ]
    )
    assert ranked[0]["growth_rate_pct"] == 0.0
    assert ranked[0]["resolved_pct"] == 0.0


def test_calculate_smart_ops_addressability():
    categories = [
        {"total_count": 182, "addressed_by_smart_ops": True},
        {"total_count": 144, "addressed_by_smart_ops": True},
        {"total_count": 110, "addressed_by_smart_ops": True},
        {"total_count": 204, "addressed_by_smart_ops": False},
    ]
    res = calculate_smart_ops_addressability(categories)
    assert res["total_analyzed"] == 640
    assert res["addressed_count"] == 436
    assert res["addressable_share_pct"] == 68.1

    empty_res = calculate_smart_ops_addressability([])
    assert empty_res["addressable_share_pct"] == 0.0


def test_build_reclame_aqui_dashboard_report_full_pipeline():
    raw_snapshot = {
        "companyName": "Mottu",
        "platform": "Reclame Aqui",
        "reputationBadge": "OTIMO",
        "overallScore": 8.2,
        "solutionRatePct": 81.4,
        "wouldDoBusinessAgainPct": 74.8,
        "avgResponseTimeHours": 18.5,
        "summary": {
            "totalComplaints": 640,
            "resolvedCount": 490,
            "unresolvedCount": 100,
            "inReplyCount": 50,
        },
        "categories": [
            {
                "categoryKey": "ORCAMENTO_PECAS_AVARIAS",
                "categoryLabel": "Previsibilidade no Valor e Quantidade de Peças",
                "previousQuarterCount": 79,
                "currentQuarterCount": 103,
                "resolvedCount": 142,
                "unresolvedCount": 26,
                "inReplyCount": 14,
                "addressedBySmartOps": True,
                "smartOpsSolution": "Orçamento Instantâneo por Foto (Loja Mottu)",
                "smartOpsRoute": "/manager/catalog",
            },
            {
                "categoryKey": "FILA_ESPERA_GALPAO",
                "categoryLabel": "Tempo de Espera e Fila Presencial no Galpão",
                "previousQuarterCount": 64,
                "currentQuarterCount": 80,
                "resolvedCount": 112,
                "unresolvedCount": 22,
                "inReplyCount": 10,
                "addressedBySmartOps": True,
                "smartOpsSolution": "Deflexão Autônoma 78,5% sem Fila",
                "smartOpsRoute": "/mechanic/queue",
            },
        ],
        "recentComplaints": [
            {
                "id": "RA-2026-9041",
                "title": "Dúvida sobre valor cobrado no retrovisor e manete",
                "categoryKey": "ORCAMENTO_PECAS_AVARIAS",
                "categoryLabel": "Previsibilidade no Valor e Quantidade de Peças",
                "status": "RESOLVED",
                "city": "São Paulo - SP",
                "yardKey": "SP-04",
                "createdAt": "2026-11-15T14:20:00Z",
                "responseTimeHours": 4.2,
                "smartOpsModule": "Orçamento Transparente T02/T04",
            }
        ],
    }

    report = build_reclame_aqui_dashboard_report(raw_snapshot, used_fallback=True)
    assert report["company_name"] == "Mottu"
    assert report["total_complaints"] == 640
    assert report["resolved_count"] == 490
    assert report["unresolved_count"] == 100
    assert report["in_reply_count"] == 50
    assert report["resolved_pct"] == 76.6
    assert report["fastest_growing_category"] == "Previsibilidade no Valor e Quantidade de Peças"
    assert report["fastest_growing_pct"] == 30.4
    assert report["addressable_share_pct"] == 100.0
    assert len(report["categories"]) == 2
    assert len(report["recent_complaints"]) == 1
    assert report["used_fallback"] is True

    # Test empty snapshot fallback
    empty_report = build_reclame_aqui_dashboard_report({}, used_fallback=False)
    assert empty_report["fastest_growing_category"] == "N/A"
    assert empty_report["fastest_growing_pct"] == 0.0

