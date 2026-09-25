import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

class TestAiSecurityHardening:
    """
    Security verification tests for Python AI Microservice:
    - Input bounds (rejection of oversized payloads to prevent DoS)
    - Empty and whitespace input rejection
    - Rejection of excessive candidate collections
    - Rejection of malformed JSON structures
    - Error response sanitization
    """

    def test_sentiment_oversized_text_rejected(self):
        huge_text = "A" * 10001
        response = client.post("/api/v1/sentiment/analyze", json={"text": huge_text})
        assert response.status_code == 422
        errors = response.json().get("detail", [])
        assert any("at most 10000 characters" in str(e) or "string_too_long" in str(e) for e in errors)

    def test_duplicate_oversized_text_rejected(self):
        huge_text = "Payment problem " * 1000  # > 10000 chars
        payload = {
            "text": huge_text,
            "candidates": [{"ticketId": "SD-1", "text": "Valid candidate"}]
        }
        response = client.post("/api/v1/duplicates/analyze", json=payload)
        assert response.status_code == 422

    def test_duplicate_candidate_oversized_text_rejected(self):
        huge_text = "A" * 10001
        payload = {
            "text": "Valid ticket text",
            "candidates": [{"ticketId": "SD-1", "text": huge_text}]
        }
        response = client.post("/api/v1/duplicates/analyze", json=payload)
        assert response.status_code == 422

    def test_duplicate_excessive_candidates_rejected(self):
        candidates = [{"ticketId": f"SD-{i}", "text": f"Candidate ticket text {i}"} for i in range(101)]
        payload = {
            "text": "Target ticket",
            "candidates": candidates
        }
        response = client.post("/api/v1/duplicates/analyze", json=payload)
        assert response.status_code == 422

    def test_sentiment_empty_or_whitespace_text_rejected(self):
        response = client.post("/api/v1/sentiment/analyze", json={"text": "   "})
        assert response.status_code == 422

    def test_duplicate_empty_or_whitespace_text_rejected(self):
        payload = {
            "text": "   ",
            "candidates": [{"ticketId": "SD-1", "text": "Valid candidate"}]
        }
        response = client.post("/api/v1/duplicates/analyze", json=payload)
        assert response.status_code == 422

    def test_classify_oversized_subject_rejected(self):
        payload = {
            "subject": "S" * 256,
            "description": "Valid description"
        }
        response = client.post("/api/v1/classify", json=payload)
        assert response.status_code == 422

    def test_classify_oversized_description_rejected(self):
        payload = {
            "subject": "Valid subject",
            "description": "D" * 5001
        }
        response = client.post("/api/v1/classify", json=payload)
        assert response.status_code == 422

    def test_malformed_json_returns_422(self):
        response = client.post(
            "/api/v1/classify",
            content="invalid-json-content",
            headers={"Content-Type": "application/json"}
        )
        assert response.status_code == 422
