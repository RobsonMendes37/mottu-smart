"""Integration tests for internal analytics advisor endpoint."""
import pytest
from fastapi.testclient import TestClient
from app.core.config import settings
from app.main import app

client = TestClient(app)


def test_analytics_advisor_unauthorized():
    response = client.post("/internal/v1/analytics/advisor", json={"currentPeriodInspections": 50})
    assert response.status_code in (401, 422)


def test_analytics_advisor_invalid_token():
    response = client.post(
        "/internal/v1/analytics/advisor",
        headers={"X-Internal-Token": "wrong-secret"},
        json={"currentPeriodInspections": 50},
    )
    assert response.status_code == 401


def test_analytics_advisor_success_flow():
    payload = {
        "yardKey": "SP-04",
        "yardLabel": "Unidade Butantã (SP-04)",
        "currentPeriodInspections": 118,
        "previousPeriodInspections": 100,
        "topMotorcycleModel": "Mottu Sport 110i",
        "partsInventory": [
            {
                "sku": "SKU-SPORT-001",
                "partName": "Manete de Freio Direito",
                "modelName": "Mottu Sport 110i",
                "stockQuantity": 4,
                "minStockQuantity": 8,
                "occurrences30d": 45,
            },
            {
                "sku": "SKU-POP-001",
                "partName": "Cabo de Embreagem",
                "modelName": "Honda Pop 110i",
                "stockQuantity": 25,
                "minStockQuantity": 8,
                "occurrences30d": 10,
            },
        ],
    }

    response = client.post(
        "/internal/v1/analytics/advisor",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json=payload,
    )

    assert response.status_code == 200
    data = response.json()

    assert data["yardKey"] == "SP-04"
    assert data["yardLabel"] == "Unidade Butantã (SP-04)"
    assert data["modelUsed"] == "gemini-3.5-flash-lite"
    assert data["demandGrowthPct"] == 18.0
    assert data["projectedNext7DaysInspections"] >= 1
    assert "executiveHeadline" in data
    assert "growthAnalysisText" in data
    assert len(data["stockoutAlerts"]) == 2
    assert data["stockoutAlerts"][0]["sku"] == "SKU-SPORT-001"
    assert data["stockoutAlerts"][0]["riskLevel"] == "CRITICAL"
    assert data["stockoutAlerts"][0]["suggestedRestockQty"] == 20
    assert len(data["recommendedActions"]) >= 2


def test_analytics_advisor_empty_inventory():
    payload = {
        "yardKey": "ALL",
        "yardLabel": "Todas as Unidades (SP)",
        "currentPeriodInspections": 50,
        "previousPeriodInspections": 50,
        "topMotorcycleModel": "Mottu Sport 110i",
        "partsInventory": [],
    }

    response = client.post(
        "/internal/v1/analytics/advisor",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json=payload,
    )

    assert response.status_code == 200
    data = response.json()
    assert data["demandGrowthPct"] == 0.0
    assert data["stockoutAlerts"] == []
    assert len(data["recommendedActions"]) >= 1



def test_analytics_ask_success():
    payload = {
        "query": "Quais peças estão com estoque crítico?",
        "yardKey": "ALL",
        "yardLabel": "Todas as Unidades (SP)",
        "contextData": {
            "parts_inventory": [
                {
                    "sku": "SKU-SPORT-001",
                    "part_name": "Manete de Freio",
                    "model_name": "Mottu Sport 110i",
                    "stock_quantity": 4,
                    "min_stock_quantity": 8,
                    "daily_burn_rate": 1.5,
                }
            ]
        },
    }

    response = client.post(
        "/internal/v1/analytics/ask",
        headers={"X-Internal-Token": settings.INTERNAL_API_TOKEN},
        json=payload,
    )

    assert response.status_code == 200
    data = response.json()
    assert "directAnswer" in data
    assert "keyMetricLabel" in data
    assert "keyMetricValue" in data
    assert len(data["columns"]) > 0
    assert len(data["rows"]) >= 1
    assert data["suggestedActionRoute"] == "/mechanic/operations"


def test_analytics_ask_unauthorized():
    response = client.post("/internal/v1/analytics/ask", json={"query": "teste"})
    assert response.status_code in (401, 422)
