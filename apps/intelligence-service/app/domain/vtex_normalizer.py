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
    "mottu sport 110i ks": MotorcycleModel.MOTTU_SPORT_110I,
    "mottu sport esd": MotorcycleModel.MOTTU_SPORT_110I,
    "sport esd": MotorcycleModel.MOTTU_SPORT_110I,
    "sport ks esd": MotorcycleModel.MOTTU_SPORT_110I,
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
    "apache 200": MotorcycleModel.TVS_APACHE_160,
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


NON_FLEET_COMPAT_VALUES = frozenset({"apache 200", "ntorq"})

_SPORT_PATTERN = re.compile(r"\bsport\b")
_POP_PATTERN = re.compile(r"\bpop\b")
_RAIDER_PATTERN = re.compile(r"\b(raider|rdr)\b")
_AP160_PATTERN = re.compile(r"\bap ?160\b")
_APACHE_PATTERN = re.compile(r"\bapache\b((?:\s+\d{3}\b)*)")
_NON_FLEET_PATTERN = re.compile(r"\b(ntorq|ap ?200)\b")


def _is_universal(normalized: str) -> bool:
    return "universal" in normalized or "geral" in normalized


def _mentions_apache_160(normalized: str) -> bool:
    if _AP160_PATTERN.search(normalized):
        return True
    match = _APACHE_PATTERN.search(normalized)
    if not match:
        return False
    displacements = match.group(1).split()
    return not displacements or "160" in displacements


def _mentions_non_fleet_model(normalized: str) -> bool:
    return bool(_NON_FLEET_PATTERN.search(normalized) or _APACHE_PATTERN.search(normalized))


def detect_models_in_name(part_name: str) -> list[MotorcycleModel]:
    """Returns fleet models explicitly mentioned in a product name, in canonical enum order."""
    normalized = normalize_text(part_name)
    mentions = {
        MotorcycleModel.MOTTU_SPORT_110I: bool(_SPORT_PATTERN.search(normalized)),
        MotorcycleModel.HONDA_POP_110I: bool(_POP_PATTERN.search(normalized)),
        MotorcycleModel.TVS_APACHE_160: _mentions_apache_160(normalized),
        MotorcycleModel.TVS_RAIDER_125: bool(_RAIDER_PATTERN.search(normalized)),
    }
    return [model for model in MotorcycleModel if mentions[model]]


def is_part_compatible_with_model(part_name: str, target_model: MotorcycleModel) -> bool:
    """Checks whether a catalog part name is compatible with the given motorcycle model."""
    normalized = normalize_text(part_name)
    if _is_universal(normalized):
        return True
    mentioned = detect_models_in_name(part_name)
    if mentioned:
        return target_model in mentioned
    return not _mentions_non_fleet_model(normalized)


def normalize_vtex_item(raw_item: Dict[str, Any], model: MotorcycleModel) -> PartCatalogItem:
    """Normalizes raw VTEX JSON product dictionary into domain PartCatalogItem."""
    name = str(raw_item.get("productName") or raw_item.get("part_name") or "").strip()
    
    sku = str(raw_item.get("sku") or raw_item.get("vtex_sku") or "").strip()
    if not sku:
        items = raw_item.get("items")
        if isinstance(items, list) and len(items) > 0 and isinstance(items[0], dict):
            sku = str(items[0].get("itemId") or "").strip()
        if not sku:
            sku = str(raw_item.get("productId") or "").strip()

    category = str(raw_item.get("category") or "").strip()
    if not category:
        categories = raw_item.get("categories")
        if isinstance(categories, list) and len(categories) > 0:
            category = str(categories[0]).strip("/").split("/")[-1].strip()
    if not category:
        category = "General"

    price_val = raw_item.get("price") if raw_item.get("price") is not None else raw_item.get("unit_price_brl")
    if price_val is None:
        items = raw_item.get("items")
        if isinstance(items, list) and len(items) > 0 and isinstance(items[0], dict):
            sellers = items[0].get("sellers")
            if isinstance(sellers, list) and len(sellers) > 0 and isinstance(sellers[0], dict):
                comm = sellers[0].get("commertialOffer")
                if isinstance(comm, dict):
                    price_val = comm.get("Price")
    if price_val is None:
        price_val = 0.0
    price = float(price_val)

    image = raw_item.get("imageUrl") or raw_item.get("image_url")
    if not image:
        items = raw_item.get("items")
        if isinstance(items, list) and len(items) > 0 and isinstance(items[0], dict):
            images = items[0].get("images")
            if isinstance(images, list) and len(images) > 0 and isinstance(images[0], dict):
                image = images[0].get("imageUrl")

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


def extract_compatible_models(raw_item: Dict[str, Any]) -> list[MotorcycleModel]:
    """Determines compatible motorcycle models from raw VTEX item or fallback dict."""
    direct = raw_item.get("motorcycle_model")
    if direct:
        parsed = parse_motorcycle_model(str(direct))
        if parsed:
            return [parsed]

    name = str(raw_item.get("productName") or raw_item.get("part_name") or "")
    found = set(detect_models_in_name(name))

    compat_list = raw_item.get("Compatibilidade da Peça")
    if isinstance(compat_list, list):
        for entry in compat_list:
            if normalize_text(str(entry)) in NON_FLEET_COMPAT_VALUES:
                continue
            parsed = parse_motorcycle_model(str(entry))
            if parsed:
                found.add(parsed)

    if found:
        return [model for model in MotorcycleModel if model in found]
    if not compat_list and _is_universal(normalize_text(name)):
        return list(MotorcycleModel)
    return []


