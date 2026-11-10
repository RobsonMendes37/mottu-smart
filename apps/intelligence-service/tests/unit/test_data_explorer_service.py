"""Unit tests for data_explorer_service pure domain service."""
from app.domain.services.data_explorer_service import (
    answer_manager_query,
    detect_plate_or_code,
    resolve_context_yard,
    is_greeting_or_casual,
)


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


def test_detect_plate_or_code_cases():
    assert detect_plate_or_code("") is None
    assert detect_plate_or_code(None) is None
    assert detect_plate_or_code("Verifique a OS-12345 no pátio") == "OS-12345"
    assert detect_plate_or_code("Consulte a moto ABC1D23") == "ABC1D23"


def test_resolve_context_yard_all_cases():
    # Direct mentions
    assert resolve_context_yard("como tá em guarulhos?", "", None) == "Unidade Guarulhos (SP-01)"
    assert resolve_context_yard("como tá em sp-01?", "", None) == "Unidade Guarulhos (SP-01)"
    assert resolve_context_yard("status interlagos", "", None) == "Unidade Interlagos (SP-03)"
    assert resolve_context_yard("status sp-03", "", None) == "Unidade Interlagos (SP-03)"
    assert resolve_context_yard("movimentação na lapa", "", None) == "Unidade Lapa (SP-02)"
    assert resolve_context_yard("movimentação sp-02", "", None) == "Unidade Lapa (SP-02)"

    # History resolution
    assert resolve_context_yard("e lá?", "Todas as Unidades (SP)", None) == "Todas as Unidades (SP)"
    assert resolve_context_yard("e por lá?", "Todas as Unidades (SP)", [{"text": "movimento em guarulhos"}]) == "Unidade Guarulhos (SP-01)"
    assert resolve_context_yard("nessa unidade?", "Todas as Unidades (SP)", [{"text": "peças em interlagos"}]) == "Unidade Interlagos (SP-03)"
    assert resolve_context_yard("nesse galpão?", "Todas as Unidades (SP)", [{"text": "como está na lapa"}]) == "Unidade Lapa (SP-02)"
    assert resolve_context_yard("e la?", "Todas as Unidades (SP)", [{"text": "geral sem filial"}]) == "Todas as Unidades (SP)"


def test_is_greeting_or_casual_cases():
    assert is_greeting_or_casual("oi") is True
    assert is_greeting_or_casual("oi monalisa") is True
    assert is_greeting_or_casual("olá!") is True
    assert is_greeting_or_casual("bom dia") is True
    assert is_greeting_or_casual("boa tarde") is True
    assert is_greeting_or_casual("tudo bem") is True
    assert is_greeting_or_casual("fodasse vc") is True
    assert is_greeting_or_casual("morra") is True
    assert is_greeting_or_casual("tchau") is True
    assert is_greeting_or_casual("opa") is True
    assert is_greeting_or_casual("muito obrigado amigo") is True
    assert is_greeting_or_casual("???") is False
    assert is_greeting_or_casual("") is False
    assert is_greeting_or_casual(None) is False
    assert is_greeting_or_casual("qual o modelo com maior giro?") is False
    assert is_greeting_or_casual("me dê um resumo geral de hoje") is False
    assert is_greeting_or_casual("quais peças estão faltando no estoque?") is False


def test_answer_manager_query_greeting_intent():
    res = answer_manager_query("oi", "Todas as Unidades (SP)", {})
    assert res["active_skill"] == "CONVERSATIONAL"
    assert res["columns"] == []
    assert res["rows"] == []
    assert res["key_metric_label"] == ""
    assert res["should_show_table"] is False
    assert "Monalisa" in res["direct_answer"]
    assert "disposição" in res["spoken_answer"] or "apoiar" in res["spoken_answer"]


def test_answer_manager_query_casual_or_insult():
    res = answer_manager_query("fodasse vc", "Todas as Unidades (SP)", {})
    assert res["active_skill"] == "CONVERSATIONAL"
    assert res["should_show_table"] is False
    assert res["columns"] == []
    assert res["rows"] == []
    assert "disposição" in res["direct_answer"]

    res_abuse = answer_manager_query("cala a boca", "Todas as Unidades (SP)", {})
    assert res_abuse["active_skill"] == "CONVERSATIONAL"
    assert res_abuse["should_show_table"] is False


def test_answer_manager_query_courtesy():
    res = answer_manager_query("obrigado", "Todas as Unidades (SP)", {})
    assert res["active_skill"] == "CONVERSATIONAL"
    assert res["should_show_table"] is False
    assert "nada" in res["direct_answer"] or "disposição" in res["direct_answer"]
    assert "Disponha" in res["spoken_answer"]

    res2 = answer_manager_query("valeu monalisa", "Todas as Unidades (SP)", {})
    assert res2["active_skill"] == "CONVERSATIONAL"
    assert res2["should_show_table"] is False


def test_answer_manager_query_identity():
    res = answer_manager_query("quem é você?", "Todas as Unidades (SP)", {})
    assert res["active_skill"] == "CONVERSATIONAL"
    assert res["should_show_table"] is False
    assert "Monalisa" in res["direct_answer"]
    assert "Monalisa" in res["spoken_answer"]

    res2 = answer_manager_query("o que você faz?", "Todas as Unidades (SP)", {})
    assert res2["active_skill"] == "CONVERSATIONAL"
    assert res2["should_show_table"] is False


def test_answer_manager_query_no_false_positive_on_article_os():
    res = answer_manager_query("quais os problemas relatados na infraestrutura?", "Todas as Unidades (SP)", {})
    assert res["active_skill"] != "SKILL_OFICINA"


def test_answer_manager_query_spoken_and_table_flags():
    res_stock = answer_manager_query("consultar estoque de peças", "Todas as Unidades (SP)", {})
    assert res_stock["should_show_table"] is True
    assert "reposição" in res_stock["spoken_answer"] or "estável" in res_stock["spoken_answer"]

    res_models = answer_manager_query("qual modelo tem mais trocas", "Todas as Unidades (SP)", {})
    assert res_models["should_show_table"] is True
    assert "Sport" in res_models["spoken_answer"]



