"""VTEX Scraper and catalog snapshot loader."""
import json
from pathlib import Path
from typing import Any, Dict, List, Tuple
import httpx
from app.core.config import settings

SNAPSHOT_PATH = Path(__file__).resolve().parent / "seed_catalog_snapshot.json"


class VTEXCatalogMiner:
    """Scrapes or falls back to seed snapshot for motorcycle parts catalog."""

    def __init__(self, base_url: str = settings.VTEX_STORE_BASE_URL):
        self.base_url = base_url

    def load_seed_snapshot(self) -> List[Dict[str, Any]]:
        """Loads embedded fallback snapshot when remote store is offline/unreachable."""
        if SNAPSHOT_PATH.exists():
            with open(SNAPSHOT_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        return []

    async def fetch_catalog(self, force_refresh: bool = False) -> Tuple[List[Dict[str, Any]], bool]:
        """Attempts to fetch from VTEX store; falls back to seed snapshot on error (UC-11)."""
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                url = f"{self.base_url}/api/catalog_system/pub/products/search"
                resp = await client.get(url, params={"_from": 0, "_to": 49})
                if resp.status_code == 200:
                    data = resp.json()
                    if isinstance(data, list) and len(data) > 0:
                        return data, False
        except Exception:
            pass
        return self.load_seed_snapshot(), True
