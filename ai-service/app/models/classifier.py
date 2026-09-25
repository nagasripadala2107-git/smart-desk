import logging
from pathlib import Path
from typing import Any, Tuple
import joblib
import numpy as np

from app.config import MODEL_PATH, MODEL_VERSION, ALLOWED_CATEGORIES

logger = logging.getLogger(__name__)

class TicketClassifier:
    def __init__(self, model_path: Path = MODEL_PATH):
        self.model_path = Path(model_path)
        self.model_version = MODEL_VERSION
        self.pipeline: Any = None
        self._is_loaded = False

    def load(self) -> None:
        """
        Loads the serialized model from disk.
        Fails fast if model file does not exist.
        """
        if not self.model_path.exists():
            error_msg = (
                f"Model file not found at '{self.model_path}'. "
                "The classifier model artifact is missing. "
                "Please run 'python training/train.py' before launching the microservice."
            )
            logger.error(error_msg)
            raise FileNotFoundError(error_msg)

        logger.info("Loading model artifact from: %s", self.model_path)
        self.pipeline = joblib.load(self.model_path)
        self._is_loaded = True
        logger.info("Model '%s' loaded successfully into memory", self.model_version)

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded and self.pipeline is not None

    def predict(self, text: str) -> Tuple[str, float]:
        """
        Predicts category and probability score for the input text.
        Returns:
            (category, confidence)
        """
        if not self.is_loaded:
            raise RuntimeError("Model is not loaded. Cannot execute inference.")

        # Obtain prediction and probability distribution
        predicted_category = self.pipeline.predict([text])[0]
        probabilities = self.pipeline.predict_proba([text])[0]
        classes = list(self.pipeline.classes_)

        idx = classes.index(predicted_category)
        confidence = float(probabilities[idx])

        # Guarantee category is strictly one of the 9 allowed categories
        if predicted_category not in ALLOWED_CATEGORIES:
            logger.warning(
                "Predicted category '%s' not in ALLOWED_CATEGORIES. Falling back to 'OTHER'",
                predicted_category,
            )
            predicted_category = "OTHER"

        return predicted_category, round(confidence, 4)

# Global singleton instance
classifier = TicketClassifier()
