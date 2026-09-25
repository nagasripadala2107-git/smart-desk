import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

HOST: str = os.getenv("HOST", "0.0.0.0")
PORT: int = int(os.getenv("PORT", "8000"))
MODEL_PATH: Path = Path(os.getenv("MODEL_PATH", str(BASE_DIR / "models" / "ticket_classifier.pkl")))
MODEL_VERSION: str = os.getenv("MODEL_VERSION", "ticket-classifier-v1")

# The 9 domain categories matching the SmartDesk Phase 2 database schema
ALLOWED_CATEGORIES: list[str] = [
    "BILLING",
    "TECHNICAL",
    "ACCOUNT",
    "REFUND",
    "SECURITY",
    "SUBSCRIPTION",
    "BUG",
    "FEATURE_REQUEST",
    "OTHER",
]

SENTIMENT_MODEL_PATH: Path = Path(os.getenv("SENTIMENT_MODEL_PATH", str(BASE_DIR / "models" / "sentiment_analyzer.pkl")))
SENTIMENT_MODEL_VERSION: str = os.getenv("SENTIMENT_MODEL_VERSION", "ticket-sentiment-v1")
ALLOWED_SENTIMENTS: list[str] = ["POSITIVE", "NEUTRAL", "NEGATIVE"]
ALLOWED_TONES: list[str] = ["CALM", "FRUSTRATED", "URGENT", "ANGRY", "SATISFIED", "CONFUSED", "NEUTRAL"]

DUPLICATE_MODEL_VERSION: str = os.getenv("DUPLICATE_MODEL_VERSION", "ticket-duplicate-v1")
DUPLICATE_THRESHOLD: float = float(os.getenv("DUPLICATE_THRESHOLD", "0.70"))
DUPLICATE_MAX_MATCHES: int = int(os.getenv("DUPLICATE_MAX_MATCHES", "5"))
