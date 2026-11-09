"""Analytics forecasting and stockout risk evaluation pure domain service.

Hexagonal Architecture Law 2: Standard Python only. Prohibited to import external frameworks.
"""
from typing import Any, Dict, List


def calculate_demand_growth(current_period_count: int, previous_period_count: int) -> Dict[str, Any]:
    """Calculate demand growth percentage and 7-day projection."""
    current = max(0, current_period_count)
    previous = max(0, previous_period_count)

    if current == 0 and previous == 0:
        growth_rate_pct = 0.0
    else:
        divisor = max(previous, 1)
        growth_rate_pct = round(((current - previous) / divisor) * 100.0, 1)

    projected_next_7d_count = max(
        1,
        round((current / 4.0) * (1.0 + max(growth_rate_pct, -30.0) / 200.0)),
    )

    return {
        "growth_rate_pct": growth_rate_pct,
        "projected_next_7d_count": int(projected_next_7d_count),
    }


def evaluate_stockout_risks(parts_usage: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Evaluate days to stockout and restock recommendations for parts."""
    evaluated: List[Dict[str, Any]] = []

    for item in parts_usage:
        sku = str(item.get("sku", ""))
        part_name = str(item.get("part_name", ""))
        model_name = str(item.get("model_name", ""))
        stock_quantity = max(0, int(item.get("stock_quantity", 0)))
        min_stock_quantity = max(0, int(item.get("min_stock_quantity", 8)))
        occurrences_30d = max(0, int(item.get("occurrences_30d", 0)))

        daily_burn_rate = round(max(occurrences_30d / 30.0, 0.2), 2)
        days_to_stockout = int(round(stock_quantity / daily_burn_rate))

        if stock_quantity <= min_stock_quantity or days_to_stockout <= 4:
            risk_level = "CRITICAL"
        elif days_to_stockout <= 8:
            risk_level = "WARNING"
        else:
            risk_level = "HEALTHY"

        if risk_level != "HEALTHY":
            suggested_restock_qty = max(10, (min_stock_quantity * 3) - stock_quantity)
        else:
            suggested_restock_qty = 0

        evaluated.append(
            {
                "sku": sku,
                "part_name": part_name,
                "model_name": model_name,
                "stock_quantity": stock_quantity,
                "min_stock_quantity": min_stock_quantity,
                "daily_burn_rate": daily_burn_rate,
                "days_to_stockout": days_to_stockout,
                "risk_level": risk_level,
                "suggested_restock_qty": suggested_restock_qty,
            }
        )

    risk_priority = {"CRITICAL": 0, "WARNING": 1, "HEALTHY": 2}
    evaluated.sort(key=lambda x: (risk_priority.get(x["risk_level"], 3), x["days_to_stockout"]))
    return evaluated


def build_deterministic_ops_advice(
    yard_label: str,
    growth_info: Dict[str, Any],
    stock_risks: List[Dict[str, Any]],
    top_model: str,
) -> Dict[str, Any]:
    """Generate canonical deterministic operational advisory for yard operations."""
    growth_pct = float(growth_info.get("growth_rate_pct", 0.0))
    projected_7d = int(growth_info.get("projected_next_7d_count", 1))

    critical_alerts = [s for s in stock_risks if s.get("risk_level") == "CRITICAL"]
    critical_count = len(critical_alerts)

    if critical_count > 0:
        headline = (
            f"Crescimento de {growth_pct}% na demanda de vistorias com alerta de reposição imediata "
            f"para {critical_count} peças em risco crítico."
        )
    else:
        headline = (
            f"Demanda de vistorias estável/equilibrada ({growth_pct}%) sem alertas críticos de ruptura "
            f"de estoque no momento."
        )

    growth_analysis_text = (
        f"O cruzamento do BigQuery (fact_inspections) indica projeção de {projected_7d} vistorias nos "
        f"próximos 7 dias para a unidade {yard_label}, com maior concentração no modelo {top_model}."
    )

    recommended_actions: List[str] = []
    for alert in critical_alerts[:3]:
        recommended_actions.append(
            f"Autorizar reposição imediata de +{alert['suggested_restock_qty']} un. de {alert['part_name']} "
            f"({alert['sku']}) para evitar parada de baia em {alert['days_to_stockout']} dias."
        )

    recommended_actions.append(
        f"Priorizar check-up preventivo de componentes de desgaste no modelo {top_model}."
    )
    recommended_actions.append(
        f"Reforçar bancada de mecânicos na unidade {yard_label} para absorver a projeção de "
        f"{projected_7d} vistorias nos próximos 7 dias."
    )

    return {
        "executive_headline": headline,
        "growth_analysis_text": growth_analysis_text,
        "stockout_alerts": stock_risks,
        "recommended_actions": recommended_actions,
    }

