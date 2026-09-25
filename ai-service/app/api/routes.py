from fastapi import APIRouter, HTTPException, status
from app.schemas.ticket_schema import (
    ClassifyRequest,
    ClassifyResponse,
    HealthResponse,
    ModelInfoResponse,
)
from app.schemas.sentiment_schema import (
    SentimentRequest,
    SentimentResponse,
)
from app.schemas.duplicate_schema import (
    DuplicateRequest,
    DuplicateResponse,
)
from app.services.classification_service import classification_service
from app.services.sentiment_service import sentiment_service
from app.services.duplicate_service import duplicate_service
from app.models.classifier import classifier
from app.models.sentiment_analyzer import sentiment_analyzer
from app.config import MODEL_VERSION, ALLOWED_CATEGORIES, SENTIMENT_MODEL_VERSION, DUPLICATE_MODEL_VERSION

router = APIRouter()

@router.get("/health", response_model=HealthResponse, tags=["Health"])
def health_check():
    """
    Returns service liveness and model readiness.
    """
    return HealthResponse(
        status="UP",
        model_loaded=classifier.is_loaded and sentiment_analyzer.is_loaded,
    )

import logging

logger = logging.getLogger(__name__)

@router.post(
    "/api/v1/classify",
    response_model=ClassifyResponse,
    status_code=status.HTTP_200_OK,
    tags=["Classification"],
)
def classify_ticket(request: ClassifyRequest):
    """
    Classifies a customer support ticket by subject and description into one of
    the 9 SmartDesk domain categories.
    """
    if not classifier.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Classifier model is not ready or failed to load.",
        )

    try:
        return classification_service.classify(request)
    except Exception as e:
        logger.error("Classification error: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Classification service encountered an unexpected error.",
        )

@router.post(
    "/api/v1/sentiment/analyze",
    response_model=SentimentResponse,
    status_code=status.HTTP_200_OK,
    tags=["Sentiment"],
)
def analyze_sentiment(request: SentimentRequest):
    """
    Analyzes customer support text (ticket or message) and predicts sentiment
    (POSITIVE, NEUTRAL, NEGATIVE), confidence score, and customer tone.
    """
    if not sentiment_analyzer.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Sentiment analyzer model is not ready or failed to load.",
        )

    try:
        return sentiment_service.analyze(request)
    except Exception as e:
        logger.error("Sentiment analysis error: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Sentiment analysis service encountered an unexpected error.",
        )

@router.post(
    "/api/v1/duplicates/analyze",
    response_model=DuplicateResponse,
    response_model_by_alias=True,
    status_code=status.HTTP_200_OK,
    tags=["Duplicates"],
)
def analyze_duplicates(request: DuplicateRequest):
    """
    Analyzes a newly created ticket text against candidate existing tickets
    and returns potential duplicate matches ranked by cosine similarity.
    """
    try:
        return duplicate_service.analyze(request)
    except Exception as e:
        logger.error("Duplicate analysis error: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Duplicate analysis service encountered an unexpected error.",
        )

@router.get(
    "/api/v1/model/info",
    response_model=ModelInfoResponse,
    tags=["Model"],
)
@router.get(
    "/api/v1/model-info",
    response_model=ModelInfoResponse,
    tags=["Model"],
)
def model_info():
    """
    Returns metadata regarding the active classifier model.
    """
    return ModelInfoResponse(
        model_version=MODEL_VERSION,
        algorithm="TfidfVectorizer + LogisticRegression (scikit-learn)",
        categories=ALLOWED_CATEGORIES,
        model_loaded=classifier.is_loaded,
    )
