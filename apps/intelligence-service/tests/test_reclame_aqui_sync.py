"""Integration tests for Reclame Aqui (VoC) analytics endpoints and Melissa SKILL_RECLAME_AQUI."""
from fastapi.testclient import TestClient
from app.core.config import settings
from app.main import app

client = TestClient(app)


def test_reclame_aqui_get_unauthorized():
    response = client.get("/internal/v1/analytics/reclame-aqui")
    assert response.status_code in (401, 422)


def test_reclame_aqui_sync_invalid_token():
    response = client.post(
        "/internal/v1/analytics/reclame-aqui/sync",
        headers={"X-Internal-Token": "wrong-token"},
        json={"forceRefresh": True},
    )
    assert response.status_code == 401


def test_reclame_aqui_get_dashboard_returns_quantified_voc_metrics():
    response = client.get(
        "/internal/v1/analytics/reclame-aqui",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["companyName"] == "Mottu"
    assert data["totalComplaints"] == 640
    assert data["resolvedCount"] == 490
    assert data["unresolvedCount"] == 100
    assert data["inReplyCount"] == 50
    assert data["resolvedPct"] == 76.6
    assert data["fastestGrowingPct"] > 25.0
    assert data["addressableSharePct"] > 60.0
    assert len(data["categories"]) == 5
    assert len(data["recentComplaints"]) >= 6


def test_reclame_aqui_post_sync_refreshes_and_returns_dashboard():
    response = client.post(
        "/internal/v1/analytics/reclame-aqui/sync",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json={"forceRefresh": False},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["totalComplaints"] == 640
    assert "lastSyncedAt" in data
    assert "message" in data


def test_melissa_skill_reclame_aqui_via_data_explorer():
    response = client.post(
        "/internal/v1/analytics/ask",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json={
            "query": "Melissa, quais são os principais problemas no Reclame Aqui e quantas foram resolvidas?",
            "yardKey": "ALL",
            "yardLabel": "Todas as Unidades (SP)",
            "skillId": "SKILL_RECLAME_AQUI",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["activeSkill"] == "SKILL_RECLAME_AQUI"
    assert data["shouldShowTable"] is True
    assert "76,6%" in data["directAnswer"] or "Reclame Aqui" in data["directAnswer"]
    assert len(data["columns"]) == 6
    assert len(data["rows"]) == 5

