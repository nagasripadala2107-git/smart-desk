from app.models.classifier import classifier
from app.preprocessing.text_cleaner import prepare_input
from app.schemas.ticket_schema import ClassifyRequest, ClassifyResponse
from app.config import MODEL_VERSION

class ClassificationService:
    @staticmethod
    def classify(request: ClassifyRequest) -> ClassifyResponse:
        combined_text = prepare_input(request.subject, request.description)
        category, confidence = classifier.predict(combined_text)

        return ClassifyResponse(
            category=category,
            confidence=confidence,
            model_version=MODEL_VERSION,
        )

classification_service = ClassificationService()
