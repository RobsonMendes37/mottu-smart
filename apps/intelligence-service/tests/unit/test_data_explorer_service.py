"""Unit tests for data_explorer_service pure domain service."""
from app.domain.services.data_explorer_service import answer_manager_query


def test_answer_manager_query_stock_intent():
    context = {
        "parts_inventory": [
            {
                "sku": "SKU-SPORT-001",
                "part_name": "Manete de Freio",
                "model_name": "Mottu Sport 110i",
                "stock_quantity": 4,
                "min_stock_quantity": 8,
                "daily_burn_rate": 1.5,
            }
        ]
    }
    res = answer_manager_query("Quais peças estão com estoque crítico?", "Todas as Unidades (SP)", context)
    assert "Peças Críticas" in res["key_metric_value"]
    assert len(res["rows"]) >= 1
    assert res["suggested_action_route"] == "/mechanic/operations"


def test_answer_manager_query_stock_intent_empty():
    context = {"parts_inventory": []}
    res = answer_manager_query("ruptura de estoque", "Unidade Butantã", context)
    assert "identificados 0 itens" in res["direct_answer"]
    assert res["columns"]


def test_answer_manager_query_verdict_intent():
    res = answer_manager_query("qual o valor de mau uso cobrado?", "Todas as Unidades (SP)", {})
    assert "Taxa de Isenção" in res["key_metric_label"]
    assert len(res["rows"]) == 3
    assert res["suggested_action_route"] == "/manager/billing"


def test_answer_manager_query_model_intent():
    res = answer_manager_query("qual modelo quebra mais?", "Todas as Unidades (SP)", {})
    assert "Mottu Sport 110i" in res["key_metric_value"]
    assert len(res["rows"]) == 4
    assert res["suggested_action_route"] == "/mechanic/fleet"


def test_answer_manager_query_yard_intent():
    res = answer_manager_query("comparar galpao de butanta e guarulhos", "Todas as Unidades (SP)", {})
    assert "Butantã" in res["key_metric_value"]
    assert len(res["rows"]) == 4


def test_answer_manager_query_billing_intent():
    res = answer_manager_query("como está o faturamento de pix e atraso?", "Todas as Unidades (SP)", {})
    assert "Faturamento Semanal" in res["key_metric_label"]
    assert len(res["rows"]) == 4
    assert res["suggested_action_route"] == "/manager/billing"


def test_answer_manager_query_default_intent():
    res = answer_manager_query("me dê um resumo geral de hoje", "Todas as Unidades (SP)", {})
    assert "Eficiência Global" in res["key_metric_label"]
    assert len(res["rows"]) == 4


def test_answer_manager_query_stock_intent_healthy_fallback():
    context = {
        "parts_inventory": [
            {
                "sku": "SKU-OK-001",
                "part_name": "Lâmpada Farol",
                "model_name": "Mottu Sport 110i",
                "stock_quantity": 25,
                "min_stock_quantity": 8,
                "daily_burn_rate": 0.5,
            }
        ]
    }
    res = answer_manager_query("consultar peças e almoxarifado", "Unidade Butantã", context)
    assert len(res["rows"]) == 1
    assert "14 dias (ESTÁVEL)" in res["rows"][0][4]


def test_answer_manager_query_plate_skill():
    res = answer_manager_query("qual a situação da placa MOT1A23?", "Todas as Unidades (SP)", {})
    assert res["active_skill"] == "SKILL_BUSCA_PLACA"
    assert "Ficha da Moto" in res["key_metric_label"]
    assert len(res["rows"]) >= 4


def test_answer_manager_query_workshop_skill():
    res = answer_manager_query("como estão as baias da oficina?", "Unidade Butantã", {}, skill_id="SKILL_OFICINA")
    assert res["active_skill"] == "SKILL_OFICINA"
    assert "Ordens de Serviço" in res["key_metric_label"]
    assert len(res["rows"]) >= 1


def test_answer_manager_query_approvals_skill():
    res = answer_manager_query("tem entregador pendente de aprovação?", "Todas as Unidades (SP)", {})
    assert res["active_skill"] == "SKILL_APROVACOES"
    assert "Fila de Aprovação" in res["key_metric_label"]


def test_answer_manager_query_memory_resolution():
    history = [
        {"sender": "user", "text": "Como está o estoque na Unidade Butantã (SP-04)?"},
        {"sender": "assistant", "text": "Temos 3 peças críticas no Butantã."},
    ]
    res = answer_manager_query("e quantas motos temos por lá?", "Todas as Unidades (SP)", {}, history=history)
    assert "Butantã" in res["direct_answer"]


def test_answer_manager_query_offline_fallback_honest():
    res = answer_manager_query(
        "qual a sua opinião sobre o mercado de delivery na américa latina?",
        "Todas as Unidades (SP)",
        {},
        is_gemini_online=False,
    )
    assert res["active_skill"] == "OFFLINE_FALLBACK"
    assert "conversação livre está offline" in res["direct_answer"]
    assert len(res["rows"]) == 7
