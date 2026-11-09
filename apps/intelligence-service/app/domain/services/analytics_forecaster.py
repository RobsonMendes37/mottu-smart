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


def categorize_part(part_name: str) -> str:
    """Categorize a motorcycle part into major operational systems."""
    name = (part_name or "").lower()
    if any(k in name for k in ["manete", "freio", "pastilha", "lona", "disco", "guidão", "guidao", "manopla", "cabo"]):
        return "BRAKE_HANDLEBAR"
    if any(k in name for k in ["pneu", "roda", "aro", "raio", "câmara", "camara"]):
        return "TIRES_WHEELS"
    if any(k in name for k in ["embreagem", "motor", "corrente", "óleo", "oleo", "eixo", "transmissão", "transmissao", "vela", "magneto", "engrenagem", "rolamento"]):
        return "ENGINE_TRANSMISSION"
    if any(k in name for k in ["carenagem", "retrovisor", "espelho", "farol", "protetor", "escapamento", "tampa", "bloco óptico", "bloco optico", "pedaleira"]):
        return "FAIRING_BODY"
    return "OTHER"


def evaluate_stockout_risks(
    parts_usage: List[Dict[str, Any]],
    horizon_days: int = 7,
    scenario: str = "NORMAL",
    system_category: str = "ALL",
) -> List[Dict[str, Any]]:
    """Evaluate days to stockout and restock recommendations for parts with scenario simulations."""
    evaluated: List[Dict[str, Any]] = []
    horizon = max(1, horizon_days)
    norm_scenario = (scenario or "NORMAL").upper()
    norm_category = (system_category or "ALL").upper()

    for item in parts_usage:
        sku = str(item.get("sku", ""))
        part_name = str(item.get("part_name", ""))
        model_name = str(item.get("model_name", ""))
        stock_quantity = max(0, int(item.get("stock_quantity", 0)))
        min_stock_quantity = max(0, int(item.get("min_stock_quantity", 8)))
        occurrences_30d = max(0, int(item.get("occurrences_30d", 0)))
        unit_price = float(item.get("unit_price_brl", 0.0) or item.get("unit_price", 0.0) or 25.0)

        category = categorize_part(part_name)
        if norm_category != "ALL" and category != norm_category:
            continue

        base_burn_rate = max(occurrences_30d / 30.0, 0.2)
        if norm_scenario == "RAIN_PEAK":
            if category in ["BRAKE_HANDLEBAR", "FAIRING_BODY", "TIRES_WHEELS"]:
                multiplier = 1.35
            else:
                multiplier = 1.10
        elif norm_scenario == "FLEET_EXPANSION":
            multiplier = 1.25
        else:
            multiplier = 1.0

        daily_burn_rate = round(base_burn_rate * multiplier, 2)
        days_to_stockout = int(round(stock_quantity / daily_burn_rate))
        projected_demand = max(1, int(round(daily_burn_rate * horizon)))

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

        estimated_cost_brl = round(suggested_restock_qty * unit_price, 2)

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
                "unit_price_brl": round(unit_price, 2),
                "estimated_cost_brl": estimated_cost_brl,
                "projected_demand": projected_demand,
                "system_category": category,
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
    horizon_days: int = 7,
    scenario: str = "NORMAL",
    system_category: str = "ALL",
) -> Dict[str, Any]:
    """Generate canonical deterministic operational advisory for yard operations with scenario support."""
    growth_pct = float(growth_info.get("growth_rate_pct", 0.0))
    projected_count = int(growth_info.get("projected_next_7d_count", 1))
    norm_scenario = (scenario or "NORMAL").upper()
    horizon = max(1, horizon_days)

    total_restock_budget_brl = round(sum(s.get("estimated_cost_brl", 0.0) for s in stock_risks), 2)
    critical_alerts = [s for s in stock_risks if s.get("risk_level") == "CRITICAL"]
    critical_count = len(critical_alerts)

    if norm_scenario == "RAIN_PEAK":
        scenario_label = "Pico de Chuva e Quedas (+35% estresse)"
        headline = (
            f"Simulação Pico de Chuva: {critical_count} peças em risco crítico nos próximos {horizon} dias. "
            f"Orçamento sugerido de R$ {total_restock_budget_brl:,.2f}."
        )
        growth_analysis_text = (
            f"O cenário de chuva acentua o desgaste de componentes de frenagem, guidão e pneus na unidade {yard_label}. "
            f"Projeção estressada para {horizon} dias indica consumo acelerado no modelo {top_model}."
        )
    elif norm_scenario == "FLEET_EXPANSION":
        scenario_label = "Expansão de Frota (+25% giro diário)"
        headline = (
            f"Simulação Expansão de Frota: giro acelerado com {critical_count} itens em risco nos próximos {horizon} dias. "
            f"Orçamento de R$ {total_restock_budget_brl:,.2f}."
        )
        growth_analysis_text = (
            f"Com a ampliação da frota ativa, a taxa de substituição de peças cresce 25% na unidade {yard_label}. "
            f"Recomenda-se antecipar pedidos ao catálogo VTEX para evitar filas na oficina."
        )
    else:
        scenario_label = "Operação Normal"
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
            f"O cruzamento do BigQuery (fact_inspections) indica projeção de {projected_count} vistorias nos "
            f"próximos {horizon} dias para a unidade {yard_label}, com maior concentração no modelo {top_model}."
        )

    recommended_actions: List[str] = []
    for alert in critical_alerts[:3]:
        recommended_actions.append(
            f"Autorizar reposição imediata de +{alert['suggested_restock_qty']} un. de {alert['part_name']} "
            f"({alert['sku']}) para evitar parada de baia em {alert['days_to_stockout']} dias."
        )

    if norm_scenario == "RAIN_PEAK":
        recommended_actions.append("Reforçar estoque de retrovisores, manetes e pastilhas de freio devido à pista molhada.")
    elif norm_scenario == "FLEET_EXPANSION":
        recommended_actions.append("Ampliar lote mínimo de segurança em 20% para absorver novas locações.")
    else:
        recommended_actions.append(f"Priorizar check-up preventivo de componentes de desgaste no modelo {top_model}.")

    recommended_actions.append(
        f"Reforçar bancada de mecânicos na unidade {yard_label} para absorver o fluxo operacional nos próximos {horizon} dias."
    )

    return {
        "executive_headline": headline,
        "growth_analysis_text": growth_analysis_text,
        "stockout_alerts": stock_risks,
        "recommended_actions": recommended_actions,
        "horizon_days": horizon,
        "scenario": norm_scenario,
        "scenario_label": scenario_label,
        "system_category": system_category,
        "total_restock_budget_brl": total_restock_budget_brl,
    }
