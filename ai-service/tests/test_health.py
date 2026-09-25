import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.classifier import classifier
from app.config import ALLOWED_CATEGORIES, MODEL_VERSION

@pytest.fixture(scope="module")
def client():
    with TestClient(app) as test_client:
        yield test_client

def test_health_endpoint(client):
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["model_loaded"] is True

def test_model_info_endpoint(client):
    response = client.get("/api/v1/model/info")
    assert response.status_code == 200
    data = response.json()
    assert data["model_version"] == MODEL_VERSION
    assert "LogisticRegression" in data["algorithm"]
    assert data["model_loaded"] is True
    assert set(data["categories"]) == set(ALLOWED_CATEGORIES)
    assert len(data["categories"]) == 9
