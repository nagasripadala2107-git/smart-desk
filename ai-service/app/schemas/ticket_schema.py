from pydantic import BaseModel, Field, field_validator
from typing import Literal

CategoryType = Literal[
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

class ClassifyRequest(BaseModel):
    subject: str = Field(..., min_length=1, max_length=255, description="Ticket subject line")
    description: str = Field(..., min_length=1, max_length=5000, description="Detailed ticket description")

    @field_validator("subject", "description")
    @classmethod
    def strip_and_validate_non_empty(cls, v: str) -> str:
        trimmed = v.strip()
        if not trimmed:
            raise ValueError("Field cannot be empty or solely whitespace")
        return trimmed

class ClassifyResponse(BaseModel):
    category: CategoryType = Field(..., description="Classified ticket domain category")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Predicted probability score")
    model_version: str = Field(..., description="Classifier model version identifier")

class HealthResponse(BaseModel):
    status: str = Field("UP", description="Microservice operational status")
    model_loaded: bool = Field(..., description="Whether the classifier artifact is loaded in memory")

class ModelInfoResponse(BaseModel):
    model_version: str
    algorithm: str
    categories: list[str]
    model_loaded: bool
