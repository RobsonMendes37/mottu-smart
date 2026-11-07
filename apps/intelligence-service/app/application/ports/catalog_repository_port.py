"""Catalog repository port interface."""
from abc import ABC, abstractmethod
from typing import List, Optional
from app.domain.enums import MotorcycleModel
from app.domain.models import PartCatalogItem


class CatalogRepositoryPort(ABC):
    @abstractmethod
    async def upsert_parts(self, parts: List[PartCatalogItem]) -> int:
        """Upsert parts into catalog, preserving price_overridden items."""
        raise NotImplementedError

    @abstractmethod
    async def find_similar_parts(
        self, model: MotorcycleModel, embedding: List[float], limit: int = 5
    ) -> List[PartCatalogItem]:
        """Find parts matching motorcycle model ordered by cosine similarity."""
        raise NotImplementedError

    @abstractmethod
    async def find_similar_parts_with_scores(
        self, model: MotorcycleModel, embedding: List[float], limit: int = 5
    ) -> List[tuple[PartCatalogItem, float]]:
        """Find parts matching motorcycle model ordered by cosine similarity with similarity scores."""
        raise NotImplementedError

