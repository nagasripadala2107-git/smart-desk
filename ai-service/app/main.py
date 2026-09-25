import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from app.api.routes import router
from app.models.classifier import classifier
from app.models.sentiment_analyzer import sentiment_analyzer
from app.config import MODEL_VERSION, SENTIMENT_MODEL_VERSION

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("smartdesk.ai")

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager that loads the model artifacts into memory once at startup.
    """
    logger.info("Initializing SmartDesk AI Microservice (%s, %s)...", MODEL_VERSION, SENTIMENT_MODEL_VERSION)
    try:
        classifier.load()
        logger.info("Classifier pipeline ready for inference.")
    except Exception as e:
        logger.critical("Failed to load classifier model on startup: %s", str(e))

    try:
        sentiment_analyzer.load()
        logger.info("Sentiment analyzer pipeline ready for inference.")
    except Exception as e:
        logger.critical("Failed to load sentiment analyzer model on startup: %s", str(e))

    yield
    logger.info("Shutting down SmartDesk AI Microservice.")

app = FastAPI(
    title="SmartDesk AI Ticket Classification Service",
    description="FastAPI microservice for deterministic classical ML classification of support tickets.",
    version="1.0.0",
    lifespan=lifespan,
)

# Global unhandled exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("Unhandled exception processing %s %s: %s", request.method, request.url, str(exc))
    return JSONResponse(
        status_code=500,
        content={"detail": "An internal error occurred during classification processing."},
    )

app.include_router(router)
