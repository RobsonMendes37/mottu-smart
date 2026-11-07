"""AI Vision port interface."""
from abc import ABC, abstractmethod
from typing import Any, Dict


class AIVisionPort(ABC):
    @abstractmethod
    async def inspect_damage_photo(self, photo_bytes: bytes, context_prompt: str) -> Dict[str, Any]:
        """Inspect a motorcycle damage photo using multimodal AI vision."""
        raise NotImplementedError

    @abstractmethod
    async def polish_mechanic_feedback(
        self, final_verdict: str, part_names: list[str], total_estimated_brl: float, mechanic_notes: str
    ) -> str:
        """Generate empathetic and clear feedback message for the rider."""
        raise NotImplementedError

    @abstractmethod
    async def generate_embedding(self, text: str) -> list[float]:
        """Generate a 768-d embedding vector for semantic search."""
        raise NotImplementedError

