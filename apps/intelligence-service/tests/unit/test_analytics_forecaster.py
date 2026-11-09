"""Unit tests for AnalyticsForecaster pure domain service (AGENTS.md Law 1 & 2)."""
import pytest
from app.domain.services.analytics_forecaster import (
    calculate_demand_growth,
    evaluate_stockout_risks,
    build_deterministic_ops_advice,
)


def test_calculate_demand_growth_normal():
    # current = 48, previous = 40 -> +20.0% growth
    res = calculate_demand_growth(current_period_count=48, previous_period_count=40)
    assert res["growth_rate_pct"] == 20.0
    # projected_next_7d_count: (48 / 4.0) * (1.0 + 20.0 / 200.0) = 12 * 1.10 = 13.2 -> 13
    assert res["projected_next_7d_count"] == 13


def test_calculate_demand_growth_zero_previous():
    # previous = 0 -> handled with max(previous, 1) = 1
    res = calculate_demand_growth(current_period_count=50, previous_period_count=0)
    assert res["growth_rate_pct"] == 5000.0
    assert res["projected_next_7d_count"] >= 1


def test_calculate_demand_growth_zero_current():
    # current = 0, previous = 20 -> -100.0%
    res = calculate_demand_growth(current_period_count=0, previous_period_count=20)
    assert res["growth_rate_pct"] == -100.0
    assert res["projected_next_7d_count"] == 1


def test_calculate_demand_growth_negative_counts():
    # Defensive against negative numbers
    res = calculate_demand_growth(current_period_count=-5, previous_period_count=-10)
    assert res["growth_rate_pct"] == 0.0
    assert res["projected_next_7d_count"] == 1


def test_evaluate_stockout_risks_empty():
    assert evaluate_stockout_risks([]) == []


def test_evaluate_stockout_risks_levels_and_sorting():
    parts = [
        {
            "sku": "SKU-HEALTHY",
            "part_name": "Retrovisor Direito",
            "model_name": "Mottu Sport 110i",
            "stock_quantity": 30,
            "min_stock_quantity": 8,
            "occurrences_30d": 15,  # burn rate = 0.5, days = 60
        },
        {
            "sku": "SKU-CRITICAL-STOCK",
            "part_name": "Manete de Freio",
            "model_name": "Mottu Sport 110i",
            "stock_quantity": 4,
            "min_stock_quantity": 8,
            "occurrences_30d": 45,  # burn rate = 1.5, days = 3 (< 4 & stock <= min)
        },
        {
            "sku": "SKU-WARNING-DAYS",
            "part_name": "Lona Traseira",
            "model_name": "Honda Pop 110i",
            "stock_quantity": 10,
            "min_stock_quantity": 8,
            "occurrences_30d": 45,  # burn rate = 1.5, days = 7 (<= 8)
        },
    ]

    evaluated = evaluate_stockout_risks(parts)
    assert len(evaluated) == 3

    # Sorting check: CRITICAL must be first, then WARNING, then HEALTHY
    assert evaluated[0]["sku"] == "SKU-CRITICAL-STOCK"
    assert evaluated[0]["risk_level"] == "CRITICAL"
    assert evaluated[0]["suggested_restock_qty"] == (8 * 3) - 4  # 20

    assert evaluated[1]["sku"] == "SKU-WARNING-DAYS"
    assert evaluated[1]["risk_level"] == "WARNING"
    assert evaluated[1]["suggested_restock_qty"] == (8 * 3) - 10  # 14

    assert evaluated[2]["sku"] == "SKU-HEALTHY"
    assert evaluated[2]["risk_level"] == "HEALTHY"
    assert evaluated[2]["suggested_restock_qty"] == 0


def test_evaluate_stockout_risks_minimum_restock_qty():
    # When min_stock * 3 - stock_quantity is small, suggested_restock_qty must be at least 10
    parts = [
        {
            "sku": "SKU-LOW-MIN",
            "part_name": "Parafuso Especial",
            "model_name": "TVS Apache 160",
            "stock_quantity": 3,
            "min_stock_quantity": 4,  # (4*3) - 3 = 9 -> max(10, 9) = 10
            "occurrences_30d": 30,
        }
    ]
    evaluated = evaluate_stockout_risks(parts)
    assert evaluated[0]["risk_level"] == "CRITICAL"
    assert evaluated[0]["suggested_restock_qty"] == 10


def test_build_deterministic_ops_advice_with_critical_alerts():
    growth = {"growth_rate_pct": 18.4, "projected_next_7d_count": 135}
    stock_risks = [
        {
            "sku": "SKU-SPORT-001",
            "part_name": "Manete de Freio Direito",
            "model_name": "Mottu Sport 110i",
            "stock_quantity": 4,
            "min_stock_quantity": 8,
            "daily_burn_rate": 1.5,
            "days_to_stockout": 3,
            "risk_level": "CRITICAL",
            "suggested_restock_qty": 20,
        }
    ]

    advice = build_deterministic_ops_advice(
        yard_label="Unidade Butantã",
        growth_info=growth,
        stock_risks=stock_risks,
        top_model="Mottu Sport 110i",
    )

    assert "18.4%" in advice["executive_headline"]
    assert "risco crítico" in advice["executive_headline"] or "alerta de reposição" in advice["executive_headline"]
    assert "135" in advice["growth_analysis_text"]
    assert len(advice["stockout_alerts"]) == 1
    assert len(advice["recommended_actions"]) >= 2
    assert any("SKU-SPORT-001" in action for action in advice["recommended_actions"])


def test_build_deterministic_ops_advice_without_critical_alerts():
    growth = {"growth_rate_pct": -2.5, "projected_next_7d_count": 80}
    stock_risks = [
        {
            "sku": "SKU-POP-001",
            "part_name": "Cabo de Embreagem",
            "model_name": "Honda Pop 110i",
            "stock_quantity": 40,
            "min_stock_quantity": 8,
            "daily_burn_rate": 0.4,
            "days_to_stockout": 100,
            "risk_level": "HEALTHY",
            "suggested_restock_qty": 0,
        }
    ]

    advice = build_deterministic_ops_advice(
        yard_label="Todas as Unidades",
        growth_info=growth,
        stock_risks=stock_risks,
        top_model="Honda Pop 110i",
    )

    assert "sem alertas críticos" in advice["executive_headline"] or "estável" in advice["executive_headline"]
    assert len(advice["recommended_actions"]) >= 1

