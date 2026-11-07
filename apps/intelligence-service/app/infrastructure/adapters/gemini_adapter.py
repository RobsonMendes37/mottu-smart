"""Gemini AI Vision adapter supporting offline mock mode (Law 8)."""
import json
import logging
from typing import Any, Dict, List
from app.application.ports.ai_vision_port import AIVisionPort
from app.core.config import settings
from app.modules.triage_ai.mock_gemini import MockGeminiService

logger = logging.getLogger(__name__)


class AIUnavailableException(Exception):
    """Raised when Gemini API is unreachable, quota-exhausted, or returns an error."""
    pass


class GeminiVisionAdapter(AIVisionPort):
    def __init__(self):
        self.mock_service = MockGeminiService()
        self.mock_mode = settings.GEMINI_MOCK_MODE
        self._client = None
        if not self.mock_mode and settings.GEMINI_API_KEY:
            try:
                from google import genai
                self._client = genai.Client(api_key=settings.GEMINI_API_KEY)
            except Exception as e:
                logger.warning(f"Failed to initialize Google GenAI Client: {e}. Falling back to mock.")
                self.mock_mode = True

    async def inspect_damage_photo(self, photo_bytes: bytes, context_prompt: str) -> Dict[str, Any]:
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return await self.mock_service.inspect_damage_photo(photo_bytes, context_prompt)

        from google.genai import types
        prompt = (
            f"You are an expert motorcycle damage triage inspector for Mottu.\n"
            f"Analyze the inspection photo considering this context:\n{context_prompt}\n"
            "Return a JSON object with:\n"
            "- isValidMotorcyclePhoto (bool)\n"
            "- confidence (float between 0.0 and 1.0)\n"
            "- suggestedVerdict ('PRE_EXISTENTE', 'DESGASTE_NATURAL', or 'MAU_USO')\n"
            "- matchedParts: list of objects with partName (str), unitPriceBrl (float), isNaturalWear (bool)\n"
            "- technicalSummary (str)\n"
            "- customerFriendlyMessage (str)\n"
        )

        last_error = None
        for model_name in settings.vision_models_list:
            try:
                logger.info(f"Inspecting damage photo with Gemini model: {model_name}")
                response = self._client.models.generate_content(
                    model=model_name,
                    contents=[
                        types.Part.from_bytes(data=photo_bytes, mime_type="image/webp"),
                        prompt,
                    ],
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json",
                    ),
                )
                if response.text and response.text.strip():
                    logger.info(f"Model {model_name} successfully inspected damage photo.")
                    return json.loads(response.text)
                logger.warning(f"Model {model_name} returned empty text, cascading to next model...")
            except Exception as e:
                logger.warning(f"Model {model_name} failed ({type(e).__name__}: {e}). Cascading to next model...")
                last_error = e

        logger.error(f"All Gemini vision models exhausted. Last error: {last_error}")
        raise AIUnavailableException(f"All Gemini vision models exhausted: {last_error}") from last_error

    async def polish_mechanic_feedback(
        self, final_verdict: str, part_names: List[str], total_estimated_brl: float, mechanic_notes: str
    ) -> str:
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return await self.mock_service.polish_mechanic_feedback(
                final_verdict, part_names, total_estimated_brl, mechanic_notes
            )

        parts_str = ", ".join(part_names)
        prompt = (
            f"Transform this mechanic's technical note into an empathetic, clear, and polite message for a motorcycle delivery rider.\n"
            f"Verdict: {final_verdict}\n"
            f"Parts: {parts_str}\n"
            f"Total Estimated BRL: R$ {total_estimated_brl:.2f}\n"
            f"Mechanic note: {mechanic_notes}\n"
            "Message must be in Brazilian Portuguese, professional, without technical jargon."
        )

        for model_name in settings.vision_models_list:
            try:
                response = self._client.models.generate_content(
                    model=model_name,
                    contents=prompt,
                )
                if response.text and response.text.strip():
                    return response.text.strip()
            except Exception as e:
                logger.warning(f"Model {model_name} failed polishing feedback ({e}). Cascading...")

        return await self.mock_service.polish_mechanic_feedback(
            final_verdict, part_names, total_estimated_brl, mechanic_notes
        )

    async def generate_embedding(self, text: str) -> List[float]:
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return self.mock_service.generate_embedding(text)

        for model_name in settings.embedding_models_list:
            try:
                response = self._client.models.embed_content(
                    model=model_name,
                    contents=text,
                )
                if hasattr(response, "embedding") and hasattr(response.embedding, "values"):
                    return list(response.embedding.values)
                elif hasattr(response, "embeddings") and len(response.embeddings) > 0:
                    return list(response.embeddings[0].values)
            except Exception as e:
                logger.warning(f"Embedding model {model_name} failed ({e}). Cascading to next model...")

        logger.warning("All embedding models exhausted. Using deterministic fallback.")
        return self.mock_service.generate_embedding(text)
