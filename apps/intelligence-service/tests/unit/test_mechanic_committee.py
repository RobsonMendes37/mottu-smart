"""Unit tests for the Multi-Specialist AI Mechanic Committee (Phase 9)."""
import pytest
from app.domain.enums import DamageVerdict
from app.domain.services.mechanic_committee import (
    MechanicCommittee,
    SpecialistEvaluation,
    WearTearSpecialist,
    ImpactForensicSpecialist,
    TemporalComparisonSpecialist
)


def test_wear_tear_specialist_identifies_high_mileage_wear():
    specialist = WearTearSpecialist()
    eval_result = specialist.evaluate(
        part_names=["PNEU TRASEIRO 90/90-18 MOTTU SPORT", "PASTILHA DE FREIO"],
        km_driven=12000,
        hours_since_pickup=720.0,
        gemini_summary="Banda de rodagem no limite do TWI sem corte ou impacto"
    )
    assert eval_result.verdict == DamageVerdict.DESGASTE_NATURAL
    assert eval_result.confidence >= 0.85
    assert "quilometragem" in eval_result.reasoning.lower() or "desgaste" in eval_result.reasoning.lower()


def test_impact_forensic_specialist_identifies_fall_marks():
    specialist = ImpactForensicSpecialist()
    eval_result = specialist.evaluate(
        part_names=["MANETE DE FREIO DIREITO", "CARENAGEM LATERAL"],
        gemini_summary="Manete torto a 45 graus com ralado profundo característico de atrito no asfalto"
    )
    assert eval_result.verdict == DamageVerdict.MAU_USO
    assert eval_result.confidence >= 0.85
    assert "impacto" in eval_result.reasoning.lower() or "asfalto" in eval_result.reasoning.lower() or "queda" in eval_result.reasoning.lower()


def test_temporal_comparison_specialist_flags_new_defect():
    specialist = TemporalComparisonSpecialist()
    eval_result = specialist.evaluate(
        part_names=["FAROL DIANTEIRO COMPLETO"],
        has_baseline_photos=True,
        baseline_summary="Moto íntegra sem avarias registradas no giro 360",
        gemini_summary="Lente do farol dianteiro estilhaçada"
    )
    assert eval_result.verdict == DamageVerdict.MAU_USO
    assert eval_result.confidence >= 0.90
    assert "360" in eval_result.reasoning or "entrega" in eval_result.reasoning.lower()


def test_committee_aggregates_unanimous_verdict():
    committee = MechanicCommittee()
    evaluation = committee.deliberate(
        part_names=["PNEU TRASEIRO MOTTU SPORT"],
        km_driven=14000,
        hours_since_pickup=800.0,
        has_baseline_photos=True,
        baseline_summary="Pneu novo",
        gemini_summary="Desgaste progressivo e simétrico por atrito de rodagem",
        is_clear_photo=True
    )
    assert evaluation["isUnanimous"] is True
    assert evaluation["consensusVerdict"] == DamageVerdict.DESGASTE_NATURAL.value
    assert evaluation["consensusScore"] == "3/3 Unânime"
    assert evaluation["canIdentifyPartsClearly"] is True
    assert evaluation["requiresPresentialInspection"] is False
    assert len(evaluation["evaluators"]) == 3


def test_committee_flags_split_vote_dissent():
    committee = MechanicCommittee()
    evaluation = committee.deliberate(
        part_names=["MANETE DE EMBREAGEM"],
        km_driven=500,
        hours_since_pickup=24.0,
        has_baseline_photos=True,
        baseline_summary="Moto íntegra",
        gemini_summary="Manete riscado na ponta e borracha gasta",
        is_clear_photo=True
    )
    assert "evaluators" in evaluation
    assert evaluation["canIdentifyPartsClearly"] is True


def test_committee_flags_inconclusive_photo_requiring_presential_workshop():
    committee = MechanicCommittee()
    evaluation = committee.deliberate(
        part_names=[],
        km_driven=3000,
        hours_since_pickup=100.0,
        has_baseline_photos=True,
        baseline_summary="Moto íntegra",
        gemini_summary="Foto escura ou com ângulo parcial, ruído interno suspeito",
        is_clear_photo=False
    )
    assert evaluation["canIdentifyPartsClearly"] is False
    assert evaluation["requiresPresentialInspection"] is True
    assert "avaliação presencial" in evaluation["summary"].lower() or "presencial" in evaluation["summary"].lower()


def test_temporal_comparison_specialist_without_baseline_photos():
    specialist = TemporalComparisonSpecialist()
    eval_result = specialist.evaluate(
        part_names=["MANETE DE FREIO"],
        has_baseline_photos=False,
        gemini_summary="Desgaste regular"
    )
    assert eval_result.verdict == DamageVerdict.DESGASTE_NATURAL
    assert eval_result.confidence == 0.75
    assert "sem histórico prévio" in eval_result.reasoning.lower()


