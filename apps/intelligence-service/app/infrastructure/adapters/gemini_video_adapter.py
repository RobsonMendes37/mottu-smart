"""Gemini AI Video Multimodal Adapter supporting offline mock mode (Law 8, UC-15)."""
import json
import logging
import os
import tempfile
from typing import Any, Dict, List, Optional
from app.core.config import settings
from app.infrastructure.adapters.gemini_adapter import AIUnavailableException
from app.modules.triage_ai.mock_gemini import MockGeminiService

logger = logging.getLogger(__name__)


class GeminiVideoAdapter:
    """Multimodal video damage analyzer using Google GenAI SDK with graceful offline mock."""

    def __init__(self):
        self.mock_service = MockGeminiService()
        self.mock_mode = settings.GEMINI_MOCK_MODE
        self._client = None
        if not self.mock_mode and settings.GEMINI_API_KEY:
            try:
                from google import genai
                self._client = genai.Client(api_key=settings.GEMINI_API_KEY)
            except Exception as e:
                logger.warning(f"Failed to initialize Google GenAI Client for Video: {e}. Falling back to mock.")
                self.mock_mode = True

    async def inspect_accident_video(self, video_bytes: bytes, context_prompt: str) -> Dict[str, Any]:
        """Analyzes a 30s accident video for damaged parts and structural issues."""
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return await self.mock_service.inspect_accident_video(video_bytes, context_prompt)

        temp_file_path = None
        uploaded_file = None
        try:
            from google.genai import types

            # For video larger than 15MB, upload via File API to prevent memory pressure
            with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as f:
                f.write(video_bytes)
                temp_file_path = f.name

            uploaded_file = self._client.files.upload(file=temp_file_path)

            prompt = (
                "You are an expert motorcycle accident inspector for Mottu.\n"
                "Analyze this accident video around the motorcycle, identifying damaged parts and estimating severity.\n"
                f"Context: {context_prompt}\n"
                "Return a JSON object with:\n"
                "- isValidVideo (bool)\n"
                "- confidence (float between 0.0 and 1.0)\n"
                "- structuralRiskAlert (bool: true if chassis, steering column, engine crankcase or fork is bent/cracked)\n"
                "- aiTechnicalSummary (str: concise 1-2 sentence assessment)\n"
                "- detectedParts: list of objects with partName (str in Brazilian Portuguese), videoTimestampSeconds (int), confidence (float)\n"
            )

            last_error = None
            for model_name in settings.vision_models_list:
                try:
                    logger.info(f"Analyzing accident video with Gemini model: {model_name}")
                    response = self._client.models.generate_content(
                        model=model_name,
                        contents=[
                            uploaded_file,
                            prompt,
                        ],
                        config=types.GenerateContentConfig(
                            response_mime_type="application/json",
                        ),
                    )
                    if response.text and response.text.strip():
                        logger.info(f"Model {model_name} successfully analyzed accident video.")
                        return json.loads(response.text)
                    logger.warning(f"Model {model_name} returned empty text for video, cascading...")
                except Exception as e:
                    logger.warning(f"Model {model_name} failed for video ({type(e).__name__}: {e}). Cascading to next model...")
                    last_error = e

            logger.error(f"All Gemini video models in cascade exhausted. Last error: {last_error}")
            raise AIUnavailableException(f"All Gemini video models exhausted: {last_error}") from last_error
        except AIUnavailableException:
            raise
        except Exception as e:
            logger.error(f"Unexpected failure during video inspection: {e}")
            raise AIUnavailableException(f"Gemini video analysis failed: {e}") from e
        finally:
            # File cleanup to prevent storage leaks
            if uploaded_file and self._client:
                try:
                    self._client.files.delete(name=uploaded_file.name)
                except Exception as del_err:
                    logger.debug(f"Failed to remove temporary Gemini file {uploaded_file.name}: {del_err}")
            if temp_file_path and os.path.exists(temp_file_path):
                try:
                    os.unlink(temp_file_path)
                except Exception:
                    pass

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

        return self.mock_service.generate_embedding(text)
