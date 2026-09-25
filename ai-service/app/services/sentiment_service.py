from app.models.sentiment_analyzer import sentiment_analyzer
from app.preprocessing.text_cleaner import clean_text
from app.schemas.sentiment_schema import SentimentRequest, SentimentResponse
from app.services.tone_detector import detect_tone
from app.config import SENTIMENT_MODEL_VERSION

class SentimentService:
    @staticmethod
    def analyze(request: SentimentRequest) -> SentimentResponse:
        cleaned = clean_text(request.text)
        sentiment, confidence = sentiment_analyzer.predict(cleaned)
        tone = detect_tone(request.text, sentiment)

        return SentimentResponse(
            sentiment=sentiment,
            confidence=confidence,
            tone=tone,
            model_version=SENTIMENT_MODEL_VERSION,
        )

sentiment_service = SentimentService()
