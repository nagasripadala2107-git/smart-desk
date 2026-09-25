import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.duplicate_detector import duplicate_detector
from app.config import DUPLICATE_MODEL_VERSION

client = TestClient(app)

def test_identical_text_similarity():
    """Identical text should yield maximum similarity score close to 1.0."""
    payload = {
        "text": "My credit card was charged twice for the same order.",
        "candidates": [
            {
                "ticketId": "SD-100",
                "text": "My credit card was charged twice for the same order."
            }
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert len(data["matches"]) == 1
    assert data["matches"][0]["ticketId"] == "SD-100"
    assert data["matches"][0]["similarity"] >= 0.99
    assert data["modelVersion"] == DUPLICATE_MODEL_VERSION

def test_highly_similar_text():
    """Highly similar text with substantial shared keywords exceeds the threshold."""
    payload = {
        "text": "My credit card was charged twice for the order today.",
        "candidates": [
            {
                "ticketId": "SD-101",
                "text": "My credit card was charged twice for the order."
            }
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert len(data["matches"]) == 1
    assert data["matches"][0]["ticketId"] == "SD-101"
    assert data["matches"][0]["similarity"] >= 0.70
    assert data["modelVersion"] == DUPLICATE_MODEL_VERSION

def test_moderately_similar_text_threshold_behavior():
    """Direct detector check with custom threshold for moderately similar text."""
    target = "Unable to connect to database server timeout error"
    candidates = [
        ("SD-201", "Database server timeout error occurred"),
        ("SD-202", "Please cancel my annual subscription plan")
    ]
    # At threshold 0.4, the technical match is returned but not the subscription match
    matches = duplicate_detector.find_duplicates(target, candidates, threshold=0.4)
    assert len(matches) == 1
    assert matches[0][0] == "SD-201"
    assert matches[0][1] >= 0.4


def test_unrelated_text_returns_no_matches():
    """Unrelated candidate texts should not meet the default 0.70 threshold."""
    payload = {
        "text": "My credit card was charged twice for the same order.",
        "candidates": [
            {
                "ticketId": "SD-301",
                "text": "How do I change my profile avatar picture?"
            },
            {
                "ticketId": "SD-302",
                "text": "The mobile app crashes on iOS 18 launch screen"
            }
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["matches"] == []
    assert data["modelVersion"] == DUPLICATE_MODEL_VERSION

def test_multiple_candidates_ranking():
    """Matches should be returned strictly in descending order of similarity."""
    target = "Payment error double charge on credit card"
    candidates = [
        ("SD-LOW", "Billing invoice receipt inquiry"),
        ("SD-HIGH", "Double charge error on credit card payment"),
        ("SD-MED", "Payment error with card processing")
    ]
    matches = duplicate_detector.find_duplicates(target, candidates, threshold=0.1)
    assert len(matches) >= 2
    for i in range(len(matches) - 1):
        assert matches[i][1] >= matches[i + 1][1]

def test_threshold_filtering():
    """Candidates with similarity strictly below threshold must be filtered out."""
    target = "Reset password link not working"
    candidates = [
        ("SD-MATCH", "Reset password link is broken not working"),
        ("SD-NOMATCH", "Upgrade to enterprise subscription plan")
    ]
    matches = duplicate_detector.find_duplicates(target, candidates, threshold=0.7)
    match_ids = [m[0] for m in matches]
    assert "SD-NOMATCH" not in match_ids

def test_maximum_result_limit():
    """Number of returned matches must not exceed max_matches (default 5)."""
    target = "Server connection timeout error"
    candidates = [
        (f"SD-{i}", f"Server connection timeout error issue {i}")
        for i in range(10)
    ]
    matches = duplicate_detector.find_duplicates(target, candidates, threshold=0.5, max_matches=5)
    assert len(matches) <= 5

def test_empty_text_validation():
    """Empty or whitespace-only text should trigger HTTP 422 validation error."""
    payload = {
        "text": "   ",
        "candidates": [
            {"ticketId": "SD-1", "text": "Valid text"}
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 422

def test_invalid_candidate_empty_id():
    """Candidate with empty ticketId should fail validation with HTTP 422."""
    payload = {
        "text": "Valid target ticket text",
        "candidates": [
            {"ticketId": "   ", "text": "Valid candidate text"}
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 422

def test_invalid_candidate_empty_text():
    """Candidate with empty text should fail validation with HTTP 422."""
    payload = {
        "text": "Valid target ticket text",
        "candidates": [
            {"ticketId": "SD-1", "text": ""}
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 422

def test_response_schema_structure():
    """Response must contain matches list and modelVersion."""
    payload = {
        "text": "Account login verification issue",
        "candidates": [
            {"ticketId": "SD-1", "text": "Account login verification issue"}
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "matches" in data
    assert "modelVersion" in data
    assert isinstance(data["matches"], list)
    assert isinstance(data["modelVersion"], str)
    if data["matches"]:
        assert "ticketId" in data["matches"][0]
        assert "similarity" in data["matches"][0]
        assert 0.0 <= data["matches"][0]["similarity"] <= 1.0

def test_model_version_identifier():
    """Model version should match ticket-duplicate-v1."""
    payload = {
        "text": "Any valid inquiry text",
        "candidates": []
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["modelVersion"] == "ticket-duplicate-v1"

def test_no_candidates_returns_empty_matches():
    """Empty candidate list returns empty matches without error."""
    payload = {
        "text": "Any valid inquiry text",
        "candidates": []
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["matches"] == []

def test_duplicate_candidate_ids_handled():
    """Duplicate candidate IDs should be processed safely without crashing."""
    payload = {
        "text": "Billing issue twice charged",
        "candidates": [
            {"ticketId": "SD-SAME", "text": "Billing issue twice charged"},
            {"ticketId": "SD-SAME", "text": "Billing issue twice charged"}
        ]
    }
    response = client.post("/api/v1/duplicates/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert len(data["matches"]) >= 1

def test_detector_is_loaded():
    """Duplicate detector is_loaded property is True."""
    assert duplicate_detector.is_loaded is True
