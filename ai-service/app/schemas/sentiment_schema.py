from pydantic import BaseModel, Field, field_validator

class SentimentRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=10000, description="Customer ticket or message text to analyze")

    @field_validator("text")
    @classmethod
    def validate_text(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Input text cannot be empty or whitespace only.")
        return v.strip()

class SentimentResponse(BaseModel):
    sentiment: str = Field(..., description="Predicted sentiment: POSITIVE, NEUTRAL, or NEGATIVE")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Model prediction confidence score")
    tone: str = Field(..., description="Detected customer tone")
    model_version: str = Field(..., description="Active sentiment model version identifier")
