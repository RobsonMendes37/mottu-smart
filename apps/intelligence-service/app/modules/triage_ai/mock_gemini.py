"""Deterministic offline mock for Gemini AI triage and feedback polish (Law 8)."""
from typing import Any, Dict, List


class MockGeminiService:
    """Mock Gemini client providing deterministic responses without external network access."""

    async def inspect_damage_photo(self, photo_bytes: bytes, context_prompt: str) -> Dict[str, Any]:
        return {
            "isValidMotorcyclePhoto": True,
            "confidence": 0.91,
            "suggestedVerdict": "PRE_EXISTENTE",
            "matchedParts": [
                {
                    "partName": "Manete de Freio",
                    "unitPriceBrl": 25.00,
                    "isNaturalWear": False,
                }
            ],
            "technicalSummary": "Ralado superficial na ponta esférica do manete direito.",
            "customerFriendlyMessage": "Olá! Identificamos o ralado no manete de freio. Como você retirou a moto há menos de 48h, o reparo é totalmente gratuito (R$ 0,00).",
        }

    async def polish_mechanic_feedback(
        self, final_verdict: str, part_names: List[str], total_estimated_brl: float, mechanic_notes: str
    ) -> str:
        parts_str = ", ".join(part_names) if part_names else "componentes inspecionados"
        if final_verdict == "DESGASTE_NATURAL":
            return (
                f"Olá! Nosso mecânico avaliou seu chamado e confirmou que a substituição de {parts_str} "
                "trata-se de desgaste natural decorrente do uso operacional. O reparo é 100% gratuito (R$ 0,00)."
            )
        elif final_verdict == "PRE_EXISTENTE":
            return (
                f"Olá! Nosso mecânico revisou a vistoria e identificou avaria pré-existente em {parts_str}. "
                "O reparo é isento de cobrança (R$ 0,00)."
            )
        else:
            return (
                f"Olá! Nosso mecânico revisou sua foto com atenção e confirmou quebra por impacto em {parts_str}. "
                f"Nota do mecânico: {mechanic_notes}. "
                f"O valor estimado para reposição no galpão é de R$ {total_estimated_brl:.2f}."
            )

    def generate_embedding(self, text: str) -> List[float]:
        """Generates a deterministic 768-d normalized unit vector for offline CRAG matching.

        Uses token-based Gaussian vector summation so semantically overlapping part names
        (e.g., 'Manete de Freio' and 'Manete de Freio Direito') produce high cosine similarity (>0.72).
        """
        import hashlib
        import math
        import random

        words = [w for w in text.strip().lower().split() if w not in ("de", "da", "do", "e", "para", "com")]
        if not words:
            words = text.strip().lower().split()
        if not words:
            return [0.0] * 768

        total = [0.0] * 768
        for word in words:
            seed = int.from_bytes(hashlib.sha256(word.encode("utf-8")).digest()[:8], "big")
            rng = random.Random(seed)
            for i in range(768):
                total[i] += rng.gauss(0.0, 1.0)

        norm = math.sqrt(sum(x * x for x in total))
        if norm > 0:
            return [round(x / norm, 6) for x in total]
        return [0.0] * 768


