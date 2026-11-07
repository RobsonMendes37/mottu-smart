"""Health Check API Integration Test."""
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_check_returns_200_and_service_info():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data == {
        "status": "UP",
        "service": "intelligence-service",
        "version": "1.0.0",
    }
