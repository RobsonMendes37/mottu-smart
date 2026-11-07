"""Pure Python VTEX item normalizer, text cleaner, and part categorization."""
import re
import unicodedata
from typing import Any, Dict, Optional
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem

CONSUMABLE_KEYWORDS = (
    "lona", "pastilha", "pneu", "oleo", "filtro", "vela",
    "corrente", "relacao", "disco de freio", "fluido", "camara de ar", "cabo",
)

CRASH_DAMAGE_KEYWORDS = (
    "carenagem", "guidao", "bengala", "tanque", "farol", "suporte",
    "chassi", "retrovisor", "espelho", "manete", "pedal", "rabeta",
    "paralama", "escapamento", "painel", "pisca", "manopla", "amortecedor",
    "mesa", "estribo", "pedaleira", "bau", "bagageiro",
)

MODEL_ALIAS_MAP: Dict[str, MotorcycleModel] = {
    "mottu sport": MotorcycleModel.MOTTU_SPORT_110I,
    "mottu sport 110": MotorcycleModel.MOTTU_SPORT_110I,
    "mottu sport 110i": MotorcycleModel.MOTTU_SPORT_110I,
    "sport 110": MotorcycleModel.MOTTU_SPORT_110I,
    "sport 110i": MotorcycleModel.MOTTU_SPORT_110I,
    "tvs sport": MotorcycleModel.MOTTU_SPORT_110I,
    "tvs sport 110": MotorcycleModel.MOTTU_SPORT_110I,
    "mottu cargo": MotorcycleModel.MOTTU_SPORT_110I,
    "honda pop": MotorcycleModel.HONDA_POP_110I,
    "honda pop 110": MotorcycleModel.HONDA_POP_110I,
    "honda pop 110i": MotorcycleModel.HONDA_POP_110I,
    "pop 110": MotorcycleModel.HONDA_POP_110I,
    "pop 110i": MotorcycleModel.HONDA_POP_110I,
    "pop": MotorcycleModel.HONDA_POP_110I,
    "tvs apache": MotorcycleModel.TVS_APACHE_160,
    "tvs apache 160": MotorcycleModel.TVS_APACHE_160,
    "apache 160": MotorcycleModel.TVS_APACHE_160,
    "apache": MotorcycleModel.TVS_APACHE_160,
    "tvs raider": MotorcycleModel.TVS_RAIDER_125,
    "tvs raider 125": MotorcycleModel.TVS_RAIDER_125,
    "raider 125": MotorcycleModel.TVS_RAIDER_125,
    "raider": MotorcycleModel.TVS_RAIDER_125,
}


def normalize_text(text: Optional[str]) -> str:
    """Strips diacritics/accents, lowercases, replaces noisy punctuation with spaces, and standardizes whitespace."""
    if not text:
        return ""
    normalized = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode("utf-8")
    lowered = normalized.lower()
    cleaned = re.sub(r"[^a-z0-9\s]", " ", lowered)
    return re.sub(r"\s+", " ", cleaned).strip()


def is_consumable_part(part_name: str) -> bool:
    """Classifies whether a part is a natural consumable wear item."""
    normalized = normalize_text(part_name)
    return any(keyword in normalized for keyword in CONSUMABLE_KEYWORDS)


def is_crash_damage_part(part_name: str) -> bool:
    """Classifies whether a part is a collision/crash structural damage item."""
    if is_consumable_part(part_name):
        return False
    normalized = normalize_text(part_name)
    return any(keyword in normalized for keyword in CRASH_DAMAGE_KEYWORDS)


def classify_damage_category(part_name: str) -> str:
    """Returns damage categorization: NATURAL_WEAR, CRASH_DAMAGE, or OTHER."""
    if is_consumable_part(part_name):
        return "NATURAL_WEAR"
    elif is_crash_damage_part(part_name):
        return "CRASH_DAMAGE"
    return "OTHER"


def parse_motorcycle_model(model_str: Optional[str]) -> Optional[MotorcycleModel]:
    """Parses raw model string or alias into canonical MotorcycleModel."""
    if not model_str:
        return None
    cleaned = normalize_text(model_str)
    for model in MotorcycleModel:
        if cleaned == normalize_text(model.value):
            return model
    return MODEL_ALIAS_MAP.get(cleaned)


def is_part_compatible_with_model(part_name: str, target_model: MotorcycleModel) -> bool:
    """Checks whether a catalog part name is compatible with the given motorcycle model."""
    normalized = normalize_text(part_name)
    if "universal" in normalized or "geral" in normalized:
        return True

    if "pop" in normalized and target_model != MotorcycleModel.HONDA_POP_110I:
        return False
    if "apache" in normalized and target_model != MotorcycleModel.TVS_APACHE_160:
        return False
    if "raider" in normalized and target_model != MotorcycleModel.TVS_RAIDER_125:
        return False
    if ("sport 110" in normalized or "tvs sport" in normalized) and target_model != MotorcycleModel.MOTTU_SPORT_110I:
        return False

    return True


def normalize_vtex_item(raw_item: Dict[str, Any], model: MotorcycleModel) -> PartCatalogItem:
    """Normalizes raw VTEX JSON product dictionary into domain PartCatalogItem."""
    name = str(raw_item.get("productName") or raw_item.get("part_name") or "").strip()
    sku = str(raw_item.get("sku") or raw_item.get("vtex_sku") or "").strip()
    category = str(raw_item.get("category") or "General").strip()
    price_val = raw_item.get("price") if raw_item.get("price") is not None else raw_item.get("unit_price_brl", 0.0)
    price = float(price_val)
    image = raw_item.get("imageUrl") or raw_item.get("image_url")

    return PartCatalogItem(
        vtex_sku=sku,
        motorcycle_model=model,
        part_name=name,
        category=category,
        unit_price_brl=price,
        is_natural_wear=is_consumable_part(name),
        price_overridden=False,
        image_url=image,
    )

