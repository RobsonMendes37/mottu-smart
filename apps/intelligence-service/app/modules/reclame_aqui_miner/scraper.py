"""Reclame Aqui (VoC) resilient miner and snapshot loader."""
import json
import logging
from pathlib import Path
from typing import Any, Dict, Tuple
import httpx

logger = logging.getLogger(__name__)

SNAPSHOT_PATH = Path(__file__).resolve().parent / "seed_reclame_aqui_snapshot.json"
RECLAME_AQUI_PUBLIC_FEED_URL = "https://iosearch.reclameaqui.com.br/raichu-io-site-search-v1/companies/mottu"


class ReclameAquiMiner:
    """Fetches public VoC telemetry or falls back deterministically to curated snapshot."""

    def __init__(self, feed_url: str = RECLAME_AQUI_PUBLIC_FEED_URL):
        self.feed_url = feed_url

    def load_seed_snapshot(self) -> Dict[str, Any]:
        """Load embedded canonical snapshot when remote feed is offline or WAF-protected."""
        if SNAPSHOT_PATH.exists():
            with open(SNAPSHOT_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        return {}

    async def fetch_complaints_snapshot(
        self, force_refresh: bool = False
    ) -> Tuple[Dict[str, Any], bool]:
        """Attempt resilient public metadata check; fall back to canonical JSON snapshot (Law 8)."""
        snapshot = self.load_seed_snapshot()
        if not force_refresh:
            return snapshot, True

        try:
            headers = {"User-Agent": "Mozilla/5.0 (compatible; MottuSmartOpsVoC/1.0)"}
            async with httpx.AsyncClient(timeout=4.0) as client:
                resp = await client.get(self.feed_url, headers=headers)
                if resp.status_code == 200:
                    remote_data = resp.json()
                    if isinstance(remote_data, dict) and "categories" in remote_data:
                        return remote_data, False
                logger.info(
                    "Reclame Aqui public endpoint returned status %s; using curated VoC snapshot",
                    resp.status_code,
                )
                return snapshot, True
        except Exception as exc:
            logger.info("Reclame Aqui remote fetch bypassed (%s), using curated VoC snapshot", exc)
            return snapshot, True

