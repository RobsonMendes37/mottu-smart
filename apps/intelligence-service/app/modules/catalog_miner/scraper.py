"""VTEX Scraper and catalog snapshot loader."""
import asyncio
import json
import logging
from pathlib import Path
from typing import Any, Dict, List, Tuple
import httpx
from app.core.config import settings

logger = logging.getLogger(__name__)

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

    async def fetch_catalog(
        self, force_refresh: bool = False, max_items: int = 1500
    ) -> Tuple[List[Dict[str, Any]], bool]:
        """Attempts to fetch from VTEX store; falls back to seed snapshot on error (UC-11)."""
        if not force_refresh:
            return self.load_seed_snapshot(), True

        try:
            headers = {"User-Agent": "Mozilla/5.0 (compatible; MottuSmartOps/1.0)"}
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"{self.base_url}/api/catalog_system/pub/products/search"
                resp0 = await client.get(url, params={"_from": 0, "_to": 49}, headers=headers)
                if resp0.status_code not in (200, 206):
                    logger.warning(f"VTEX returned unexpected status {resp0.status_code}, falling back to snapshot")
                    return self.load_seed_snapshot(), True

                data0 = resp0.json()
                if not isinstance(data0, list) or len(data0) == 0:
                    return self.load_seed_snapshot(), True

                total_available = len(data0)
                res_header = resp0.headers.get("resources", "")
                if "/" in res_header:
                    try:
                        total_available = int(res_header.split("/")[-1])
                    except (ValueError, TypeError):
                        pass

                target_count = min(total_available, max_items)
                if target_count <= 50:
                    return data0[:target_count], False

                all_items = list(data0)
                sem = asyncio.Semaphore(5)

                async def fetch_page(start: int, end: int):
                    async with sem:
                        r = await client.get(url, params={"_from": start, "_to": end}, headers=headers)
                        if r.status_code in (200, 206):
                            return r.json()
                        return []

                ranges = [(i, min(i + 49, target_count - 1)) for i in range(50, target_count, 50)]
                pages = await asyncio.gather(*[fetch_page(s, e) for s, e in ranges])
                for p in pages:
                    if isinstance(p, list):
                        all_items.extend(p)

                return all_items, False
        except Exception as e:
            logger.warning(f"Error scraping VTEX catalog ({e}), using fallback snapshot")
            return self.load_seed_snapshot(), True
