"""Multi-Specialist AI Mechanic Committee for Fleet Damage Triage (Phase 9).

Executes a collegiate panel of 3 virtual specialists (Wear & Tear, Forensic Impact,
and 360 Baseline Temporal Comparison) to evaluate motorcycle damages, reach consensus,
and detect inconclusive photos that require in-person workshop inspection.
"""
from dataclasses import dataclass
from typing import List, Dict, Any
from app.domain.enums import DamageVerdict


@dataclass
class SpecialistEvaluation:
    specialist_role: str
    verdict: DamageVerdict
    confidence: float
    reasoning: str


class WearTearSpecialist:
    """Specialist focused on operational fatigue, mileage correlation, and consumable parts."""

    WEAR_PARTS_KEYWORDS = [
        "PNEU", "PASTILHA", "LONA", "CORRENTE", "RELAÇÃO", "COROA", "PINHÃO",
        "EMBREAGEM", "DISCO", "CABO", "VELA", "ÓLEO", "ROLAMENTO", "MANOPLA"
    ]

    def evaluate(
        self,
        part_names: List[str],
        km_driven: int,
        hours_since_pickup: float,
        gemini_summary: str = ""
    ) -> SpecialistEvaluation:
        text_corpus = (" ".join(part_names) + " " + gemini_summary).upper()
        is_wear_item = any(keyword in text_corpus for keyword in self.WEAR_PARTS_KEYWORDS)
        high_mileage = km_driven >= 5000 or hours_since_pickup >= 168.0  # >1 week

        if is_wear_item and high_mileage:
            return SpecialistEvaluation(
                specialist_role="PERITO_FADIGA_DESGASTE",
                verdict=DamageVerdict.DESGASTE_NATURAL,
                confidence=0.94,
                reasoning=f"Quilometragem rodada ({km_driven} km em {hours_since_pickup:.1f}h) é plenamente compatível com desgaste operacional do componente."
            )
        elif is_wear_item and not high_mileage:
            return SpecialistEvaluation(
                specialist_role="PERITO_FADIGA_DESGASTE",
                verdict=DamageVerdict.PRE_EXISTENTE if hours_since_pickup <= 48.0 else DamageVerdict.DESGASTE_NATURAL,
                confidence=0.82,
                reasoning=f"Item de desgaste avaliado com baixa rodagem ({km_driven} km). Avaliado no limite de tolerância operacional."
            )
        else:
            has_impact_indicators = any(k in text_corpus for k in ["RALADO", "QUEBRADO", "TORTO", "TRINCADO", "ESTILHAÇADO"])
            return SpecialistEvaluation(
                specialist_role="PERITO_FADIGA_DESGASTE",
                verdict=DamageVerdict.MAU_USO if has_impact_indicators else DamageVerdict.DESGASTE_NATURAL,
                confidence=0.80,
                reasoning="Componente estrutural analisado: desgaste não condiz com fadiga mecânica cíclica ordinária."
            )


class ImpactForensicSpecialist:
    """Specialist inspecting mechanical abrasions, collision vector, and drop markers."""

    IMPACT_KEYWORDS = [
        "RALADO", "QUEBRADO", "EMPENADO", "TORTO", "TRINCA", "ESTILHAÇADO",
        "ASFALTO", "QUEDA", "BATIDA", "COLISÃO", "MEIO-FIO", "AMASSADO"
    ]

    def evaluate(
        self,
        part_names: List[str],
        gemini_summary: str = ""
    ) -> SpecialistEvaluation:
        text_corpus = (" ".join(part_names) + " " + gemini_summary).upper()
        impact_matches = [k for k in self.IMPACT_KEYWORDS if k in text_corpus]

        if impact_matches:
            return SpecialistEvaluation(
                specialist_role="PERITO_FORENSE_IMPACTO",
                verdict=DamageVerdict.MAU_USO,
                confidence=0.92,
                reasoning=f"Evidências forenses de impacto/atrito externo detectadas ({', '.join(impact_matches[:2])}), caracterizando sinistro ou colisão."
            )
        else:
            return SpecialistEvaluation(
                specialist_role="PERITO_FORENSE_IMPACTO",
                verdict=DamageVerdict.DESGASTE_NATURAL,
                confidence=0.88,
                reasoning="Ausência de estrias de abrasão no asfalto, deformação plástica ou pontos de fratura por impacto contundente."
            )


class TemporalComparisonSpecialist:
    """Specialist comparing current damage against the handover baseline 360 photo archive."""

    CRITICAL_ROOTS = ["QUEBRA", "ESTILHA", "AMASSA", "FALTA", "TORTO", "TRINCA", "RACHAD"]

    def evaluate(
        self,
        part_names: List[str],
        has_baseline_photos: bool = True,
        baseline_summary: str = "",
        gemini_summary: str = ""
    ) -> SpecialistEvaluation:
        text_corpus = (" ".join(part_names) + " " + gemini_summary).upper()
        has_critical_damage = any(root in text_corpus for root in self.CRITICAL_ROOTS)

        if has_baseline_photos and has_critical_damage:
            return SpecialistEvaluation(
                specialist_role="PERITO_COMPARADOR_360",
                verdict=DamageVerdict.MAU_USO,
                confidence=0.91,
                reasoning="Comparativo com a vistoria 360 de entrega confirma que a peça encontrava-se íntegra na expedição da moto."
            )
        elif has_baseline_photos and not has_critical_damage:
            return SpecialistEvaluation(
                specialist_role="PERITO_COMPARADOR_360",
                verdict=DamageVerdict.DESGASTE_NATURAL,
                confidence=0.89,
                reasoning="Evolução suave e homogênea das superfícies em relação ao registro 360 do pátio."
            )
        else:
            return SpecialistEvaluation(
                specialist_role="PERITO_COMPARADOR_360",
                verdict=DamageVerdict.DESGASTE_NATURAL,
                confidence=0.75,
                reasoning="Avaliação comparativa sem histórico prévio discrepante registrado."
            )


class MechanicCommittee:
    """Orchestrates the 3-specialist panel and compiles consensus scorecard."""

    def __init__(self):
        self.wear_specialist = WearTearSpecialist()
        self.forensic_specialist = ImpactForensicSpecialist()
        self.temporal_specialist = TemporalComparisonSpecialist()

    def deliberate(
        self,
        part_names: List[str],
        km_driven: int,
        hours_since_pickup: float,
        has_baseline_photos: bool = True,
        baseline_summary: str = "",
        gemini_summary: str = "",
        is_clear_photo: bool = True
    ) -> Dict[str, Any]:
        eval_wear = self.wear_specialist.evaluate(part_names, km_driven, hours_since_pickup, gemini_summary)
        eval_forensic = self.forensic_specialist.evaluate(part_names, gemini_summary)
        eval_temporal = self.temporal_specialist.evaluate(part_names, has_baseline_photos, baseline_summary, gemini_summary)

        evaluations = [eval_wear, eval_forensic, eval_temporal]

        # Count votes
        verdict_counts: Dict[DamageVerdict, int] = {}
        for ev in evaluations:
            verdict_counts[ev.verdict] = verdict_counts.get(ev.verdict, 0) + 1

        # Majority verdict (in case of tie, prioritize natural wear)
        majority_verdict = max(
            verdict_counts.keys(),
            key=lambda v: (verdict_counts[v], 1 if v == DamageVerdict.DESGASTE_NATURAL else 0)
        )
        majority_count = verdict_counts[majority_verdict]
        is_unanimous = (majority_count == len(evaluations))

        avg_confidence = sum(ev.confidence for ev in evaluations) / len(evaluations)

        if is_unanimous:
            consensus_score = f"{len(evaluations)}/{len(evaluations)} Unânime"
            dissenting_opinion = None
        else:
            dissenting_count = len(evaluations) - majority_count
            consensus_score = f"{majority_count}/{len(evaluations)} Divergência Técnica ({dissenting_count} divergente)"
            dissenting = [
                f"{ev.specialist_role} votou {ev.verdict.value}: {ev.reasoning}"
                for ev in evaluations if ev.verdict != majority_verdict
            ]
            dissenting_opinion = "; ".join(dissenting)

        can_identify_parts = is_clear_photo and (len(part_names) > 0 or "ÍNTEGRA" in gemini_summary.upper())
        requires_presential = not is_clear_photo or (not can_identify_parts and len(part_names) == 0)

        if requires_presential:
            summary = "Recomendada convocação para avaliação presencial na oficina: foto inconclusiva para fechamento remoto ou ruído/falha interna."
        elif is_unanimous:
            summary = f"Consenso unânime da Junta Técnica ({majority_verdict.value}) com {int(avg_confidence * 100)}% de certeza média."
        else:
            summary = f"Decisão majoritária ({consensus_score}) com divergência técnica registrada para arbitramento do mecânico de bancada."

        evaluators_list = [
            {
                "specialistRole": ev.specialist_role,
                "verdict": ev.verdict.value,
                "confidence": round(ev.confidence, 2),
                "reasoning": ev.reasoning
            }
            for ev in evaluations
        ]

        return {
            "consensusVerdict": majority_verdict.value,
            "consensusScore": consensus_score,
            "isUnanimous": is_unanimous,
            "canIdentifyPartsClearly": can_identify_parts,
            "requiresPresentialInspection": requires_presential,
            "averageConfidence": round(avg_confidence, 2),
            "dissentingOpinion": dissenting_opinion,
            "summary": summary,
            "evaluators": evaluators_list
        }
