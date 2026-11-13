"""Gemini AI Vision adapter supporting offline mock mode (Law 8)."""
import asyncio
import json
import logging
from typing import Any, Dict, List, Optional
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

    @property
    def is_mock_mode(self) -> bool:
        return self.mock_mode

    @property
    def is_online(self) -> bool:
        return not self.mock_mode and self._client is not None

    async def inspect_damage_photo(self, photo_bytes: bytes, context_prompt: str) -> Dict[str, Any]:
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return await self.mock_service.inspect_damage_photo(photo_bytes, context_prompt)

        from google.genai import types
        prompt = (
            "Você é um perito técnico e avaliador sênior de triagem de avarias de motocicletas para a Mottu.\n"
            f"Analise a foto da vistoria considerando este contexto operacional:\n{context_prompt}\n\n"
            "Diretrizes obrigatórias de linguagem:\n"
            "- TODAS as respostas, descrições e mensagens DEVEM ser estritamente em Português do Brasil (pt-BR).\n"
            "- Se a imagem NÃO for de uma motocicleta ou estiver ilegível, retorne isValidMotorcyclePhoto como false e em customerFriendlyMessage explique de forma educada e clara que a foto não é de uma moto ou não permitiu identificar o veículo.\n"
            "- customerFriendlyMessage: mensagem empática e profissional para o entregador parceiro, sem jargões complexos e SEM qualquer formatação markdown (sem asteriscos **, sem hashtags #, sem marcadores hifenizados).\n\n"
            "Retorne EXCLUSIVAMENTE um objeto JSON com as seguintes chaves:\n"
            "- isValidMotorcyclePhoto (bool): true se for foto nítida de motocicleta/componente de moto, false se for foto de objeto alheio, animal, pessoa, paisagem ou totalmente ilegível\n"
            "- confidence (float entre 0.0 e 1.0): grau de certeza técnica da identificação\n"
            "- suggestedVerdict ('PRE_EXISTENTE', 'DESGASTE_NATURAL' ou 'MAU_USO')\n"
            "- matchedParts: lista de objetos contendo partName (str em português), unitPriceBrl (float), isNaturalWear (bool)\n"
            "- technicalSummary (str em português): síntese técnica da avaria para o mecânico\n"
            "- customerFriendlyMessage (str em português): mensagem clara e acolhedora enviada ao entregador\n"
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

        parts_str = ", ".join(part_names) if part_names else "componentes avaliados"
        prompt = (
            "Escreva uma mensagem humana, acolhedora, respeitosa e clara para um entregador parceiro da Mottu.\n"
            f"Parecer da oficina: {final_verdict}\n"
            f"Peças avaliadas: {parts_str}\n"
            f"Valor estimado do reparo: R$ {total_estimated_brl:.2f}\n"
            f"Observação técnica do mecânico: {mechanic_notes}\n"
            "Regras obrigatórias de formatação:\n"
            "- Idioma: Português do Brasil, tom profissional e empático.\n"
            "- PROIBIDO usar sintaxe de Markdown (não use asteriscos **, #, marcadores de lista com hífen ou sublinhados).\n"
            "- Escreva em parágrafos diretos, limpos e fluidos, adequados para visualização no aplicativo mobile.\n"
            "- Explique com transparência o que foi constatado e os próximos passos para o entregador."
        )

        import re

        def sanitize_feedback(raw: str) -> str:
            cleaned = re.sub(r"\*\*(.*?)\*\*", r"\1", raw)
            cleaned = re.sub(r"\*(.*?)\*", r"\1", cleaned)
            cleaned = re.sub(r"^#{1,6}\s+", "", cleaned, flags=re.MULTILINE)
            cleaned = re.sub(r"^\s*[-*]\s+", "• ", cleaned, flags=re.MULTILINE)
            return cleaned.strip()

        for model_name in settings.vision_models_list:
            try:
                response = self._client.models.generate_content(
                    model=model_name,
                    contents=prompt,
                )
                if response.text and response.text.strip():
                    return sanitize_feedback(response.text)
            except Exception as e:
                logger.warning(f"Model {model_name} failed polishing feedback ({e}). Cascading...")

        fallback = await self.mock_service.polish_mechanic_feedback(
            final_verdict, part_names, total_estimated_brl, mechanic_notes
        )
        return sanitize_feedback(fallback)

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

    async def generate_ops_advisor_insights(
        self, deterministic_advice: Dict[str, Any], top_model: str, yard_label: str
    ) -> Dict[str, Any]:
        """Optionally enrich deterministic operations advice with Gemini 3.5 Flash Lite if online."""
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return deterministic_advice

        prompt = (
            f"You are the Mottu AI Ops Executive Advisor.\n"
            f"Context: Yard '{yard_label}', top inspected motorcycle model '{top_model}'.\n"
            f"Data summary:\n"
            f"- Base headline: {deterministic_advice.get('executive_headline')}\n"
            f"- Growth analysis: {deterministic_advice.get('growth_analysis_text')}\n"
            f"- Recommended actions: {json.dumps(deterministic_advice.get('recommended_actions', []))}\n"
            "Generate an executive, professional analysis in Brazilian Portuguese with:\n"
            "{\n"
            '  "executive_headline": "string",\n'
            '  "growth_analysis_text": "string",\n'
            '  "recommended_actions": ["action 1", "action 2", "action 3"]\n'
            "}"
        )

        for model_name in settings.vision_models_list:
            try:
                from google.genai import types

                def call_model():
                    return self._client.models.generate_content(
                        model=model_name,
                        contents=prompt,
                        config=types.GenerateContentConfig(response_mime_type="application/json"),
                    )

                response = await asyncio.wait_for(asyncio.to_thread(call_model), timeout=3.5)
                if response.text and response.text.strip():
                    parsed = json.loads(response.text)
                    return {
                        "executive_headline": parsed.get(
                            "executive_headline", deterministic_advice.get("executive_headline")
                        ),
                        "growth_analysis_text": parsed.get(
                            "growth_analysis_text", deterministic_advice.get("growth_analysis_text")
                        ),
                        "stockout_alerts": deterministic_advice.get("stockout_alerts", []),
                        "recommended_actions": parsed.get(
                            "recommended_actions", deterministic_advice.get("recommended_actions", [])
                        ),
                    }
            except Exception as e:
                logger.warning(f"Ops advisor enrichment failed with {model_name} ({e}). Cascading...")

        return deterministic_advice

    async def answer_manager_query_with_llm(
        self,
        query: str,
        yard_label: str,
        context_data: Dict[str, Any],
        history: Optional[List[Dict[str, Any]]] = None,
        default_answer: str = "",
    ) -> Optional[Dict[str, Any]]:
        if self.mock_mode or not settings.GEMINI_API_KEY or self._client is None:
            return None

        parts = context_data.get("parts_inventory", [])
        parts_summary = f"{len(parts)} peças monitoradas"
        critical_parts = [p.get("partName", "") for p in parts if p.get("currentStock", 0) <= p.get("safetyStock", 0)]
        critical_str = ", ".join(critical_parts[:4]) if critical_parts else "estoque sem alertas críticos"

        # Identifica se o usuário perguntou sobre um SKU ou peça específica
        part_context_str = ""
        q_upper = query.upper()
        for p in parts:
            p_sku = str(p.get("sku", p.get("vtexSku", ""))).upper()
            p_name = str(p.get("part_name", p.get("partName", "")))
            if (p_sku and p_sku in q_upper) or (p_name and len(p_name) >= 4 and p_name.lower() in query.lower()):
                stock_qty = p.get("stock_quantity", p.get("currentStock", 0))
                min_stock = p.get("min_stock_quantity", p.get("safetyStock", 8))
                model = p.get("model_name", p.get("modelName", ""))
                part_context_str = f"- Peça específica identificada no inventário: SKU {p_sku} ({p_name} - {model}) conta com saldo de {stock_qty} unidades em estoque no galpão (estoque mínimo de segurança: {min_stock} un.).\n"
                break

        orders = context_data.get("service_orders", [])
        orders_summary = f"{len(orders)} ordens abertas na oficina"

        history_lines = []
        if history:
            for m in history[-4:]:
                sender = "Gestor" if m.get("sender") == "user" else "Monalisa"
                history_lines.append(f"{sender}: {m.get('text', '')}")
        history_str = "\n".join(history_lines) if history_lines else "Início da conversa."

        prompt = (
            "Você é a Monalisa, a copiloto técnica e operacional de frotas e oficinas da Mottu.\n"
            "Sua comunicação é extremamente direta, profissional e concisa. Responda estritamente à pergunta do gestor em 1 a 2 frases.\n"
            "NUNCA cite números, métricas de contexto ou frotas se a pergunta não for especificamente sobre elas.\n\n"
            f"Dados Operacionais de {yard_label} (Apenas referência interna de suporte):\n"
            f"- Peças: {parts_summary} ({critical_str})\n"
            f"{part_context_str}"
            f"- Oficina: {orders_summary}, fila liberada\n"
            "- Vistorias: 78.5% deflexão sem fila, operação estável\n\n"
            f"Histórico recente:\n{history_str}\n\n"
            f"Mensagem do Usuário: {query}\n\n"
            "Instrução Crítica: Se o usuário perguntou sobre a quantidade ou saldo de uma peça ou código SKU, informe obrigatoriamente a quantidade exata encontrada nos dados operacionais.\n\n"
            "Retorne estritamente um JSON no seguinte formato:\n"
            "{\n"
            '  "direct_answer": "Texto objetivo em 1 ou no máximo 2 frases. Sem markdown, sem hashtags, sem emojis.",\n'
            '  "spoken_answer": "Frase falada ultra-curta e natural para voz (máximo 12 palavras).",\n'
            '  "should_show_table": false\n'
            "}\n\n"
            "Regras Inquebráveis:\n"
            "1. Defina 'should_show_table' como true APENAS se o usuário pedir explicitamente para ver lista, tabela, histórico de modelos ou comparação detalhada.\n"
            "2. Se a pergunta for sobre estabilidade operacional, infraestrutura sem falhas ou estoque abastecido, responda de forma direta e defina should_show_table como false.\n"
            "3. PROIBIDO usar formatação markdown: sem asteriscos **, sem hashtags #, sem marcadores hifenizados. PROIBIDO emojis."
        )

        for model_name in settings.vision_models_list:
            try:
                from google.genai import types

                def call_query_model():
                    return self._client.models.generate_content(
                        model=model_name,
                        contents=prompt,
                        config=types.GenerateContentConfig(response_mime_type="application/json"),
                    )

                response = await asyncio.wait_for(asyncio.to_thread(call_query_model), timeout=4.0)
                if response.text and response.text.strip():
                    raw_text = response.text.strip()
                    import re
                    try:
                        clean_json = raw_text
                        if "```json" in clean_json:
                            clean_json = clean_json.split("```json")[1].split("```")[0].strip()
                        elif "```" in clean_json:
                            clean_json = clean_json.split("```")[1].split("```")[0].strip()

                        data = json.loads(clean_json)
                        if isinstance(data, dict):
                            direct = str(data.get("direct_answer", "")).strip()
                            spoken = str(data.get("spoken_answer", "")).strip() or direct
                            should_show = bool(data.get("should_show_table", False))
                            direct = re.sub(r"\*\*(.*?)\*\*", r"\1", direct)
                            direct = re.sub(r"^#{1,6}\s+", "", direct, flags=re.MULTILINE)
                            spoken = re.sub(r"\*\*(.*?)\*\*", r"\1", spoken)
                            spoken = re.sub(r"^#{1,6}\s+", "", spoken, flags=re.MULTILINE)
                            return {
                                "direct_answer": direct,
                                "spoken_answer": spoken,
                                "should_show_table": should_show,
                            }
                    except Exception:
                        pass

                    cleaned = re.sub(r"\*\*(.*?)\*\*", r"\1", raw_text)
                    cleaned = re.sub(r"\*(.*?)\*", r"\1", cleaned)
                    cleaned = re.sub(r"^#{1,6}\s+", "", cleaned, flags=re.MULTILINE)
                    cleaned = re.sub(r"^\s*[-*]\s+", "", cleaned, flags=re.MULTILINE)
                    clean_str = cleaned.strip()
                    first_sent = clean_str.split(".")[0] + "." if "." in clean_str else clean_str
                    return {
                        "direct_answer": clean_str,
                        "spoken_answer": first_sent[:80],
                        "should_show_table": False,
                    }
            except Exception as e:
                logger.warning(f"Model {model_name} failed answering manager query: {e}. Cascading...")

        return None

