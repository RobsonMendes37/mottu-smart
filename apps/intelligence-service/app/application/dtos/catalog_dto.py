"""Catalog sync request and response DTOs."""
from datetime import datetime
from typing import Optional
from pydantic import BaseModel


class CatalogSyncRequestDTO(BaseModel):
    forceRefresh: bool = False


class CatalogSyncResponseDTO(BaseModel):
    syncedCount: int
    usedFallback: bool
    syncedAt: datetime
    message: Optional[str] = None
