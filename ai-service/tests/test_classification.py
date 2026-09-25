import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import ALLOWED_CATEGORIES
from app.models.classifier import classifier

@pytest.fixture(scope="module")
def client():
    with TestClient(app) as test_client:
        yield test_client

def test_classify_billing(client):
    payload = {
        "subject": "Payment charged twice on credit card",
        "description": "My bank statement shows a duplicate charge of $49.99 for my subscription.",
    }
    response = client.post("/api/v1/classify", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["category"] == "BILLING"
    assert 0.0 <= data["confidence"] <= 1.0
    assert data["model_version"] == "ticket-classifier-v1"

def test_classify_technical(client):
    payload = {
        "subject": "Database connection timeout 504",
        "description": "The postgres connection pool is exhausted during request spikes.",
    }
    response = client.post("/api/v1/classify", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["category"] == "TECHNICAL"
    assert 0.0 <= data["confidence"] <= 1.0

def test_classify_refund(client):
    payload = {
        "subject": "Requesting refund for purchase",
        "description": "I would like to get my money back because the service does not fit our team.",
    }
    response = client.post("/api/v1/classify", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["category"] == "REFUND"
    assert 0.0 <= data["confidence"] <= 1.0

def test_classify_account(client):
    payload = {
        "subject": "Forgot password and cannot login",
        "description": "Need to reset credentials for my customer account access.",
    }
    response = client.post("/api/v1/classify", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["category"] == "ACCOUNT"
    assert 0.0 <= data["confidence"] <= 1.0

def test_classify_security(client):
    payload = {
        "subject": "Suspicious login from unknown IP address",
        "description": "Someone accessed our admin account without authorization.",
    }
    response = client.post("/api/v1/classify", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["category"] == "SECURITY"
    assert 0.0 <= data["confidence"] <= 1.0

def test_returned_category_is_always_in_allowed_set(client):
    examples = [
        {"subject": "Subscription renewal", "description": "How do I upgrade to enterprise annual tier?"},
        {"subject": "App crash bug", "description": "Uploading large images throws a white screen exception."},
        {"subject": "Feature suggestion", "description": "Please add dark mode theme option."},
        {"subject": "General inquiry", "description": "Where is your office headquarters located?"},
    ]
    for ex in examples:
        response = client.post("/api/v1/classify", json=ex)
        assert response.status_code == 200
        data = response.json()
        assert data["category"] in ALLOWED_CATEGORIES

def test_classify_validation_errors(client):
    # Empty subject
    res1 = client.post("/api/v1/classify", json={"subject": "   ", "description": "Valid description"})
    assert res1.status_code == 422

    # Missing description
    res2 = client.post("/api/v1/classify", json={"subject": "Valid subject"})
    assert res2.status_code == 422

    # Empty payload
    res3 = client.post("/api/v1/classify", json={})
    assert res3.status_code == 422

def test_missing_model_behavior(client, monkeypatch):
    # Simulate unready model
    monkeypatch.setattr(classifier, "_is_loaded", False)
    payload = {
        "subject": "Valid Subject",
        "description": "Valid description text",
    }
    response = client.post("/api/v1/classify", json=payload)
    assert response.status_code == 503
    assert "not ready" in response.json()["detail"].lower()
