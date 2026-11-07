"""Unit tests for VTEX normalizer domain logic (UC-09, UC-11)."""
import pytest
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem
from app.domain.vtex_normalizer import (
    normalize_text,
    is_consumable_part,
    is_crash_damage_part,
    classify_damage_category,
    parse_motorcycle_model,
    is_part_compatible_with_model,
    normalize_vtex_item,
)


# =============================================================================
# 1. Text Normalization Pipeline Tests
# =============================================================================

def test_normalize_text_accents_and_diacritics():
    assert normalize_text("Óleo 10W30 Mineral") == "oleo 10w30 mineral"
    assert normalize_text("Vela de Ignição CPR8EA") == "vela de ignicao cpr8ea"
    assert normalize_text("Kit Relação Completa") == "kit relacao completa"
    assert normalize_text("Pedal de Câmbio") == "pedal de cambio"
    assert normalize_text("Guidão Esportivo") == "guidao esportivo"
    assert normalize_text("Cabeçote & Pistão") == "cabecote pistao"


def test_normalize_text_punctuation_and_whitespace():
    raw = "  Manete de Freio / Dianteiro - (Sport 110i) & Suporte [LED]... "
    expected = "manete de freio dianteiro sport 110i suporte led"
    assert normalize_text(raw) == expected


def test_normalize_text_empty_and_none():
    assert normalize_text("") == ""
    assert normalize_text(None) == ""
    assert normalize_text("    ") == ""


# =============================================================================
# 2. Consumable Wear vs Crash Damage Categorization Tests
# =============================================================================

@pytest.mark.parametrize(
    "part_name",
    [
        "Lona de Freio Traseira",
        "Pastilha de Freio Dianteira",
        "Pneu Dianteiro 80/100-18",
        "Pneu Traseiro 90/90-18",
        "Óleo de Motor 10W30 4T",
        "Filtro de Ar Pop",
        "Vela de Ignição CPR8EA",
        "Corrente de Transmissão",
        "Kit Relação Completa Apache",
        "Disco de Freio Dianteiro",
        "Fluido de Freio DOT4",
        "Câmara de Ar Aro 18",
        "Cabo de Acelerador",
        "Cabo de Embreagem",
    ],
)
def test_is_consumable_part_positive(part_name: str):
    assert is_consumable_part(part_name) is True


@pytest.mark.parametrize(
    "part_name",
    [
        "Manete de Freio Direito",
        "Espelho Retrovisor Esquerdo",
        "Carenagem Lateral Vermelha",
        "Guidão Original",
        "Bengala Dianteira Esquerda",
        "Tanque de Combustível",
        "Farol Principal LED",
        "Suporte de Placa",
        "Chassi Tubular",
        "Pedal de Câmbio",
        "Pedal de Freio",
        "Rabeta Traseira",
        "Paralama Dianteiro",
        "Escapamento Esportivo",
        "Painel Digital",
        "Pisca Traseiro Direito",
        "Manopla de Borracha",
    ],
)
def test_is_consumable_part_negative_for_structural(part_name: str):
    assert is_consumable_part(part_name) is False


@pytest.mark.parametrize(
    "part_name",
    [
        "Carenagem Lateral",
        "Guidão Cromado",
        "Bengala Suspensão",
        "Tanque Gasolina",
        "Farol Principal LED",
        "Suporte do Motor",
        "Chassi",
        "Espelho Retrovisor",
        "Manete de Embreagem",
        "Pedal de Câmbio",
        "Rabeta",
        "Paralama Traseiro",
        "Escapamento Original",
        "Painel de Instrumentos",
        "Pisca Alerta",
        "Manopla Esquerda",
        "Amortecedor Traseiro",
        "Mesa Superior",
        "Estribo Lateral",
        "Pedaleira Traseira",
        "Baú 45 Litros",
        "Bagageiro Reforçado",
    ],
)
def test_is_crash_damage_part_positive(part_name: str):
    assert is_crash_damage_part(part_name) is True


def test_is_crash_damage_part_negative_for_consumable():
    # Consumables should never be classified as crash damage
    assert is_crash_damage_part("Lona de Freio") is False
    assert is_crash_damage_part("Óleo de Motor 10W30") is False
    assert is_crash_damage_part("Pneu Traseiro") is False
    assert is_crash_damage_part("Vela de Ignição") is False


def test_classify_damage_category():
    assert classify_damage_category("Lona de Freio") == "NATURAL_WEAR"
    assert classify_damage_category("Óleo 10W30") == "NATURAL_WEAR"
    assert classify_damage_category("Farol Principal LED") == "CRASH_DAMAGE"
    assert classify_damage_category("Manete de Freio") == "CRASH_DAMAGE"
    assert classify_damage_category("Adesivo Decorativo") == "OTHER"
    assert classify_damage_category("Chaveiro Promocional") == "OTHER"


# =============================================================================
# 3. Model Parsing & Compatibility Tests
# =============================================================================

def test_parse_motorcycle_model_exact_enums():
    assert parse_motorcycle_model("MOTTU_SPORT_110I") == MotorcycleModel.MOTTU_SPORT_110I
    assert parse_motorcycle_model("HONDA_POP_110I") == MotorcycleModel.HONDA_POP_110I
    assert parse_motorcycle_model("TVS_APACHE_160") == MotorcycleModel.TVS_APACHE_160
    assert parse_motorcycle_model("TVS_RAIDER_125") == MotorcycleModel.TVS_RAIDER_125


def test_parse_motorcycle_model_aliases():
    # Mottu Sport aliases
    assert parse_motorcycle_model("Mottu Sport") == MotorcycleModel.MOTTU_SPORT_110I
    assert parse_motorcycle_model("mottu sport 110") == MotorcycleModel.MOTTU_SPORT_110I
    assert parse_motorcycle_model("TVS Sport 110") == MotorcycleModel.MOTTU_SPORT_110I
    assert parse_motorcycle_model("sport 110i") == MotorcycleModel.MOTTU_SPORT_110I
    assert parse_motorcycle_model("Mottu Cargo") == MotorcycleModel.MOTTU_SPORT_110I

    # Honda Pop aliases
    assert parse_motorcycle_model("Honda Pop") == MotorcycleModel.HONDA_POP_110I
    assert parse_motorcycle_model("pop 110i") == MotorcycleModel.HONDA_POP_110I
    assert parse_motorcycle_model("pop") == MotorcycleModel.HONDA_POP_110I

    # Apache aliases
    assert parse_motorcycle_model("TVS Apache") == MotorcycleModel.TVS_APACHE_160
    assert parse_motorcycle_model("apache 160") == MotorcycleModel.TVS_APACHE_160
    assert parse_motorcycle_model("apache") == MotorcycleModel.TVS_APACHE_160

    # Raider aliases
    assert parse_motorcycle_model("TVS Raider") == MotorcycleModel.TVS_RAIDER_125
    assert parse_motorcycle_model("raider 125") == MotorcycleModel.TVS_RAIDER_125
    assert parse_motorcycle_model("raider") == MotorcycleModel.TVS_RAIDER_125


def test_parse_motorcycle_model_invalid_or_none():
    assert parse_motorcycle_model(None) is None
    assert parse_motorcycle_model("") is None
    assert parse_motorcycle_model("Yamaha Fazer 250") is None
    assert parse_motorcycle_model("Modelo Desconhecido") is None


def test_is_part_compatible_with_model():
    # Universal / Generic parts
    assert is_part_compatible_with_model("Óleo 10W30 Universal", MotorcycleModel.MOTTU_SPORT_110I) is True
    assert is_part_compatible_with_model("Fluido Geral de Freio", MotorcycleModel.HONDA_POP_110I) is True

    # Same model matches
    assert is_part_compatible_with_model("Filtro de Ar Pop", MotorcycleModel.HONDA_POP_110I) is True
    assert is_part_compatible_with_model("Kit Relação Apache", MotorcycleModel.TVS_APACHE_160) is True
    assert is_part_compatible_with_model("Manete Raider 125", MotorcycleModel.TVS_RAIDER_125) is True
    assert is_part_compatible_with_model("Pedal TVS Sport 110", MotorcycleModel.MOTTU_SPORT_110I) is True

    # Incompatible models (competing mentions)
    assert is_part_compatible_with_model("Filtro de Ar Pop", MotorcycleModel.MOTTU_SPORT_110I) is False
    assert is_part_compatible_with_model("Kit Relação Apache", MotorcycleModel.HONDA_POP_110I) is False
    assert is_part_compatible_with_model("Farol Raider", MotorcycleModel.TVS_APACHE_160) is False
    assert is_part_compatible_with_model("Manete Sport 110", MotorcycleModel.HONDA_POP_110I) is False


# =============================================================================
# 4. VTEX Item Normalization Tests
# =============================================================================

def test_normalize_vtex_item_standard_fields():
    raw = {
        "sku": "SKU-VTEX-001",
        "productName": "Óleo de Motor 10W30 4T",
        "category": "Lubrificantes",
        "price": 35.0,
        "imageUrl": "https://loja.mottu.com.br/oleo.jpg",
    }
    item = normalize_vtex_item(raw, MotorcycleModel.MOTTU_SPORT_110I)
    assert isinstance(item, PartCatalogItem)
    assert item.vtex_sku == "SKU-VTEX-001"
    assert item.part_name == "Óleo de Motor 10W30 4T"
    assert item.motorcycle_model == MotorcycleModel.MOTTU_SPORT_110I
    assert item.category == "Lubrificantes"
    assert item.unit_price_brl == 35.0
    assert item.is_natural_wear is True
    assert item.price_overridden is False
    assert item.image_url == "https://loja.mottu.com.br/oleo.jpg"


def test_normalize_vtex_item_alternate_field_names():
    raw = {
        "vtex_sku": "SKU-ALT-002",
        "part_name": "Manete de Freio Direito",
        "unit_price_brl": 25.0,
        "image_url": "https://loja.mottu.com.br/manete.jpg",
    }
    item = normalize_vtex_item(raw, MotorcycleModel.HONDA_POP_110I)
    assert item.vtex_sku == "SKU-ALT-002"
    assert item.part_name == "Manete de Freio Direito"
    assert item.unit_price_brl == 25.0
    assert item.is_natural_wear is False


def test_normalize_vtex_item_defaults():
    item = normalize_vtex_item({}, MotorcycleModel.TVS_APACHE_160)
    assert item.vtex_sku == ""
    assert item.part_name == ""
    assert item.category == "General"
    assert item.unit_price_brl == 0.0
    assert item.is_natural_wear is False
    assert item.image_url is None


def test_normalize_vtex_item_negative_price_raises_error():
    raw = {"sku": "ERR-1", "productName": "Item", "price": -10.0}
    with pytest.raises(ValueError, match="unit_price_brl cannot be negative"):
        normalize_vtex_item(raw, MotorcycleModel.MOTTU_SPORT_110I)


# =============================================================================
# 5. Catalog Snapshot Verification (24 Parts Across 4 Models)
# =============================================================================

def test_catalog_seed_parts_classification():
    # Mottu Sport 110i sample
    assert not is_consumable_part("Manete de Freio Direito")
    assert not is_consumable_part("Espelho Retrovisor Direito")
    assert is_consumable_part("Lona de Freio Traseira")
    assert is_consumable_part("Pneu Dianteiro 80/100-18")
    assert is_consumable_part("Vela de Ignição CPR8EA")
    assert not is_consumable_part("Pedal de Câmbio")

    # Honda Pop 110i sample
    assert not is_consumable_part("Manete de Freio Dianteiro")
    assert not is_consumable_part("Espelho Retrovisor Esquerdo")
    assert is_consumable_part("Lona de Freio Traseira Pop")
    assert is_consumable_part("Pneu Traseiro 80/100-14")
    assert is_consumable_part("Filtro de Ar Pop")
    assert not is_consumable_part("Pisca Traseiro Direito")

    # TVS Apache 160 sample
    assert not is_consumable_part("Manete de Embreagem")
    assert not is_consumable_part("Espelho Retrovisor Esportivo")
    assert is_consumable_part("Pastilha de Freio Dianteira")
    assert is_consumable_part("Pneu Traseiro 110/80-17")
    assert is_consumable_part("Kit Relação Completa Apache")
    assert not is_consumable_part("Pedal de Freio Traseiro")

    # TVS Raider 125 sample
    assert not is_consumable_part("Manete de Freio Raider")
    assert not is_consumable_part("Espelho Retrovisor Raider")
    assert is_consumable_part("Pastilha de Freio Raider")
    assert is_consumable_part("Pneu Dianteiro 90/90-17")
    assert is_consumable_part("Óleo de Motor 10W30 4T")
    assert not is_consumable_part("Farol Principal LED")
