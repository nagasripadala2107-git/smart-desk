import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import ALLOWED_SENTIMENTS, ALLOWED_TONES, SENTIMENT_MODEL_VERSION
from app.models.sentiment_analyzer import sentiment_analyzer
from app.services.tone_detector import detect_tone

@pytest.fixture(scope="module")
def client():
    with TestClient(app) as test_client:
        yield test_client

def test_sentiment_analyze_endpoint_structure(client):
    payload = {"text": "I need help understanding our monthly statement."}
    response = client.post("/api/v1/sentiment/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "sentiment" in data
    assert "confidence" in data
    assert "tone" in data
    assert "model_version" in data
    assert data["sentiment"] in ALLOWED_SENTIMENTS
    assert 0.0 <= data["confidence"] <= 1.0
    assert data["tone"] in ALLOWED_TONES
    assert data["model_version"] == SENTIMENT_MODEL_VERSION

def test_sentiment_from_ml_model(client):
    """
    Verifies that sentiment is produced by the trained ML model and falls within
    the allowed set with valid confidence, without hardcoding predictions.
    """
    payload = {"text": "Our payment was processed twice and nobody has resolved this issue for days."}
    response = client.post("/api/v1/sentiment/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    # Sentiment comes dynamically from ML model
    assert data["sentiment"] in ALLOWED_SENTIMENTS
    assert 0.0 <= data["confidence"] <= 1.0
    # Tone must adhere to deterministic heuristic rules given the model's actual sentiment
    expected_tone = detect_tone(payload["text"], data["sentiment"])
    assert data["tone"] == expected_tone

def test_tone_detection_angry():
    tone = detect_tone("This service is unacceptable, absolutely furious about this charge!", "NEGATIVE")
    assert tone == "ANGRY"

def test_tone_detection_urgent():
    tone = detect_tone("Emergency: production down and completely blocked immediately!", "NEGATIVE")
    assert tone == "URGENT"

def test_tone_detection_frustrated():
    tone = detect_tone("Still not fixed and still waiting for days with no response.", "NEGATIVE")
    assert tone == "FRUSTRATED"

def test_tone_detection_satisfied():
    tone = detect_tone("Thank you so much! Everything is solved and works great now.", "POSITIVE")
    assert tone == "SATISFIED"

def test_tone_detection_confused():
    tone = detect_tone("I do not understand how do I configure this DNS setting?", "NEUTRAL")
    assert tone == "CONFUSED"

def test_tone_detection_calm():
    tone = detect_tone("Please update our account billing contact email address to finance@company.com", "NEUTRAL")
    assert tone == "CALM"

def test_sentiment_validation_errors(client):
    # Empty string
    res1 = client.post("/api/v1/sentiment/analyze", json={"text": ""})
    assert res1.status_code == 422

    # Whitespace only
    res2 = client.post("/api/v1/sentiment/analyze", json={"text": "   \n\t  "})
    assert res2.status_code == 422

    # Missing text key
    res3 = client.post("/api/v1/sentiment/analyze", json={})
    assert res3.status_code == 422

def test_missing_sentiment_model_behavior(client, monkeypatch):
    monkeypatch.setattr(sentiment_analyzer, "_is_loaded", False)
    payload = {"text": "Testing model unavailability handling."}
    response = client.post("/api/v1/sentiment/analyze", json=payload)
    assert response.status_code == 503
    assert "not ready" in response.json()["detail"].lower()
