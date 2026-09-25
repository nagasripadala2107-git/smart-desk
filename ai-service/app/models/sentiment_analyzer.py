import logging
from pathlib import Path
from typing import Any, Tuple
import joblib

from app.config import SENTIMENT_MODEL_PATH, SENTIMENT_MODEL_VERSION, ALLOWED_SENTIMENTS

logger = logging.getLogger(__name__)

class SentimentAnalyzer:
    def __init__(self, model_path: Path = SENTIMENT_MODEL_PATH):
        self.model_path = Path(model_path)
        self.model_version = SENTIMENT_MODEL_VERSION
        self.pipeline: Any = None
        self._is_loaded = False

    def load(self) -> None:
        """
        Loads the serialized sentiment model from disk.
        Fails fast if model file does not exist.
        """
        if not self.model_path.exists():
            error_msg = (
                f"Sentiment model file not found at '{self.model_path}'. "
                "The sentiment model artifact is missing. "
                "Please run 'python training/train_sentiment.py' before launching the microservice."
            )
            logger.error(error_msg)
            raise FileNotFoundError(error_msg)

        logger.info("Loading sentiment model artifact from: %s", self.model_path)
        self.pipeline = joblib.load(self.model_path)
        self._is_loaded = True
        logger.info("Sentiment model '%s' loaded successfully into memory", self.model_version)

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded and self.pipeline is not None

    def predict(self, text: str) -> Tuple[str, float]:
        """
        Predicts sentiment and probability score for the input text.
        Returns:
            (sentiment, confidence)
        """
        if not self.is_loaded:
            raise RuntimeError("Sentiment model is not loaded. Cannot execute inference.")

        predicted_sentiment = self.pipeline.predict([text])[0]
        probabilities = self.pipeline.predict_proba([text])[0]
        classes = list(self.pipeline.classes_)

        idx = classes.index(predicted_sentiment)
        confidence = float(probabilities[idx])

        if predicted_sentiment not in ALLOWED_SENTIMENTS:
            logger.warning(
                "Predicted sentiment '%s' not in ALLOWED_SENTIMENTS. Falling back to 'NEUTRAL'",
                predicted_sentiment,
            )
            predicted_sentiment = "NEUTRAL"

        return predicted_sentiment, round(confidence, 4)

# Global singleton instance
sentiment_analyzer = SentimentAnalyzer()
