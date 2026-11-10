"""Redis worker adapter and background queue supervisor (docs/02 §6.5, UC-08)."""
import asyncio
import json
import logging
from typing import Any, Dict, List, Optional
import httpx
import redis.asyncio as redis
from app.application.dtos.triage_job_dto import TriageJobPayloadDTO
from app.application.ports.queue_port import QueuePort
from app.core.config import settings
from app.domain.crag_evaluator import CRAG_AMBIGUITY_DELTA, CRAG_MIN_SIMILARITY_THRESHOLD
from app.domain.enums import MotorcycleModel
from app.domain.services.mechanic_committee import MechanicCommittee
from app.domain.vtex_normalizer import is_consumable_part, parse_motorcycle_model
from app.infrastructure.adapters.gemini_adapter import AIUnavailableException, GeminiVisionAdapter
from app.infrastructure.adapters.postgres_pgvector import PostgresPgVectorAdapter

logger = logging.getLogger(__name__)


class RedisQueueAdapter(QueuePort):
    def __init__(self):
        self.host = settings.REDIS_HOST
        self.port = settings.REDIS_PORT
        self.jobs_queue = "queue:ai_triage_jobs"
        self.results_queue = "queue:ai_triage_results"

    async def _get_client(self):
        return redis.Redis(
            host=self.host,
            port=self.port,
            decode_responses=True,
            socket_connect_timeout=2.0,
            socket_timeout=2.0,
        )

    async def pop_triage_job(self, timeout_seconds: int = 2) -> Optional[Dict[str, Any]]:
        client = None
        try:
            client = await self._get_client()
            item = await client.brpop(self.jobs_queue, timeout=timeout_seconds)
            if item:
                return json.loads(item[1])
            return None
        except Exception as e:
            logger.debug(f"Redis pop_triage_job idle or unreachable: {e}")
            return None
        finally:
            if client:
                await client.aclose()

    async def push_triage_result(self, result: Dict[str, Any]) -> None:
        client = None
        try:
            client = await self._get_client()
            await client.lpush(self.results_queue, json.dumps(result))
        except Exception as e:
            logger.warning(f"Redis push_triage_result failed: {e}")
        finally:
            if client:
                await client.aclose()


class TriageJobWorker:
    """Consumes queue:ai_triage_jobs, runs Gemini Vision + CRAG pgvector, and produces to queue:ai_triage_results."""

    def __init__(
        self,
        queue_adapter: Optional[RedisQueueAdapter] = None,
        vision_adapter: Optional[GeminiVisionAdapter] = None,
        catalog_adapter: Optional[PostgresPgVectorAdapter] = None,
    ):
        self.queue_adapter = queue_adapter or RedisQueueAdapter()
        self.vision_adapter = vision_adapter or GeminiVisionAdapter()
        self.catalog_adapter = catalog_adapter or PostgresPgVectorAdapter()
        self.committee = MechanicCommittee()
        self.is_running: bool = False
        self._stop_event = asyncio.Event()

    def start(self):
        self.is_running = True
        self._stop_event.clear()

    def stop(self):
        self.is_running = False
        self._stop_event.set()

    async def run_loop(self):
        self.start()
        logger.info("TriageJobWorker started consumer loop on queue:ai_triage_jobs")
        while self.is_running:
            try:
                job = await self.queue_adapter.pop_triage_job(timeout_seconds=2)
                if job:
                    await self.process_single_job(job)
                else:
                    await asyncio.sleep(0.1)
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Unexpected error in TriageJobWorker loop: {e}", exc_info=True)
                await asyncio.sleep(1.0)
        logger.info("TriageJobWorker stopped consumer loop")

    async def _fetch_photo_bytes(self, photo_object_key: str) -> bytes:
        """Fetches photo from MinIO / S3 storage via HTTP or returns dummy bytes in offline mode."""
        url = f"{settings.S3_INTERNAL_ENDPOINT}/{settings.S3_BUCKET_NAME}/{photo_object_key}"
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                resp = await client.get(url)
                if resp.status_code == 200 and resp.content:
                    return resp.content
        except Exception:
            pass
        # Fallback offline 1x1 image payload
        return b"RIFF\x1a\x00\x00\x00WEBPVP8 \x0e\x00\x00\x00\xb0\x01\x00\x9d\x01*\x01\x00\x01\x00\x00"

    async def process_single_job(self, raw_job: Dict[str, Any]) -> bool:
        """Processes one inbound triage job. Returns True on success, False on fallback or failure."""
        try:
            job = TriageJobPayloadDTO(**raw_job)
        except Exception as e:
            logger.error(f"Invalid job payload discarded: {raw_job} error: {e}")
            return False

        try:
            photo_bytes = await self._fetch_photo_bytes(job.photoObjectKey)
            context_prompt = (
                f"Model: {job.motorcycleModel}, Context: {job.reportedContext}, "
                f"Hours: {job.hoursSincePickup}, KM: {job.kmDrivenSincePickup}, "
                f"Notes: {job.riderNotes or 'None'}"
            )
            triage_raw = await self.vision_adapter.inspect_damage_photo(photo_bytes, context_prompt)
        except (AIUnavailableException, Exception) as e:
            logger.warning(f"AI inspection unavailable for inspection {job.inspectionId}: {e}. Triggering AI_UNAVAILABLE.")
            fallback_payload = {
                "inspectionId": job.inspectionId,
                "correlationId": job.correlationId,
                "outcome": "AI_UNAVAILABLE",
            }
            await self.queue_adapter.push_triage_result(fallback_payload)
            return False

        # Parse motorcycle model
        model_enum = parse_motorcycle_model(job.motorcycleModel) or MotorcycleModel.MOTTU_SPORT_110I
        initial_confidence = float(triage_raw.get("confidence", 0.90))
        adjusted_confidence = initial_confidence

        # Process matched parts and CRAG cosine similarity evaluation
        raw_parts = triage_raw.get("matchedParts", [])
        matched_parts_dto: List[Dict[str, Any]] = []
        has_natural_wear = False

        for p in raw_parts:
            part_name = p.get("partName", "Peça Desconhecida")
            unit_price = float(p.get("unitPriceBrl", 0.0))
            is_wear = bool(p.get("isNaturalWear", False)) or is_consumable_part(part_name)
            if is_wear:
                has_natural_wear = True

            catalog_part_id = p.get("catalogPartId")

            # CRAG pgvector search
            emb = await self.vision_adapter.generate_embedding(part_name)
            candidates = await self.catalog_adapter.find_similar_parts_with_scores(model_enum, emb, limit=5)
            if candidates:
                top_item, top_sim = candidates[0]
                if not catalog_part_id:
                    catalog_part_id = top_item.vtex_sku
                if unit_price <= 0:
                    unit_price = top_item.unit_price_brl

                # Evaluate CRAG thresholds
                if top_sim < CRAG_MIN_SIMILARITY_THRESHOLD:
                    adjusted_confidence = min(adjusted_confidence, 0.75)
                elif len(candidates) > 1:
                    second_item, second_sim = candidates[1]
                    if (top_sim - second_sim) < CRAG_AMBIGUITY_DELTA and top_item.vtex_sku != second_item.vtex_sku:
                        adjusted_confidence = min(adjusted_confidence, 0.75)

            matched_parts_dto.append({
                "catalogPartId": catalog_part_id,
                "partName": part_name,
                "unitPriceBrl": unit_price,
                "isNaturalWear": is_wear,
            })

        is_valid_photo = bool(triage_raw.get("isValidMotorcyclePhoto", True))
        tech_summary = triage_raw.get("technicalSummary", "Vistoria analisada pela IA.")
        part_names = [p["partName"] for p in matched_parts_dto]

        committee_eval = self.committee.deliberate(
            part_names=part_names,
            km_driven=job.kmDrivenSincePickup,
            hours_since_pickup=job.hoursSincePickup,
            has_baseline_photos=True,
            baseline_summary="Giro 360 de entrega da moto",
            gemini_summary=tech_summary,
            is_clear_photo=is_valid_photo and adjusted_confidence >= 0.60
        )

        if not is_valid_photo:
            suggested_verdict = "MAU_USO"
            customer_message = (
                "Olá! A imagem enviada não parece ser de uma motocicleta ou está muito escura/ilegível para identificação. "
                "Por favor, tire uma nova foto bem iluminada focando no componente da moto que precisa de avaliação."
            )
        # Inconclusive photo protection: zero automatic charge
        elif not committee_eval.get("canIdentifyPartsClearly", True):
            suggested_verdict = committee_eval.get("consensusVerdict", "DESGASTE_NATURAL")
            customer_message = (
                "Olá! Registramos sua foto. Para sua total proteção, como não foi possível identificar 100% das peças com precisão remota, "
                "nenhuma cobrança automática foi gerada e nosso mecânico fará a triagem técnica na oficina."
            )
        # Apply 48h / 100km exemption rule (UC-08, UC-11)
        elif job.hoursSincePickup <= 48.0 and job.kmDrivenSincePickup <= 100 and job.reportedContext in ("RECEIVED_LIKE_THIS", "WITHDRAWAL"):
            suggested_verdict = "PRE_EXISTENTE"
            customer_message = (
                f"Olá! Identificamos o dano apontado. Como você retirou a moto há menos de 48h "
                f"({job.hoursSincePickup:.1f}h) e rodou {job.kmDrivenSincePickup} km, o reparo é totalmente isento de cobrança (R$ 0,00)."
            )
        elif has_natural_wear or committee_eval.get("consensusVerdict") == "DESGASTE_NATURAL":
            suggested_verdict = "DESGASTE_NATURAL"
            customer_message = (
                "Olá! Identificamos que a peça indicada trata-se de desgaste natural pelo uso regular. "
                "A substituição na oficina é 100% gratuita (R$ 0,00)."
            )
        else:
            suggested_verdict = committee_eval.get("consensusVerdict", "MAU_USO")
            customer_message = triage_raw.get("customerFriendlyMessage") or (
                "Olá! Identificamos avaria decorrente de uso/impacto recente. Encaminhado para revisão técnica no galpão."
            )

        success_result = {
            "inspectionId": job.inspectionId,
            "correlationId": job.correlationId,
            "outcome": "SUCCESS",
            "isValidMotorcyclePhoto": is_valid_photo,
            "confidence": round(adjusted_confidence, 2),
            "suggestedVerdict": suggested_verdict,
            "matchedParts": matched_parts_dto,
            "technicalSummary": tech_summary,
            "customerFriendlyMessage": customer_message,
            "committeeEvaluation": committee_eval,
        }

        await self.queue_adapter.push_triage_result(success_result)
        return True
