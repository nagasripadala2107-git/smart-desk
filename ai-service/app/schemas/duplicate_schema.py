from pydantic import BaseModel, Field, field_validator, ConfigDict

class DuplicateCandidate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ticket_id: str = Field(..., alias="ticketId", min_length=1, max_length=255, description="Candidate ticket identifier")
    text: str = Field(..., min_length=1, max_length=10000, description="Candidate ticket subject and description text")

    @field_validator("ticket_id", "text")
    @classmethod
    def validate_non_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Field cannot be empty or whitespace only.")
        return v.strip()

class DuplicateRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=10000, description="Target ticket text to find duplicates for")
    candidates: list[DuplicateCandidate] = Field(default_factory=list, max_length=100, description="List of candidate tickets to compare against")

    @field_validator("text")
    @classmethod
    def validate_text(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Input text cannot be empty or whitespace only.")
        return v.strip()

class DuplicateMatch(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ticket_id: str = Field(..., alias="ticketId", description="Candidate ticket identifier")
    similarity: float = Field(..., ge=0.0, le=1.0, description="Cosine similarity score (0.0 to 1.0)")

class DuplicateResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    matches: list[DuplicateMatch] = Field(default_factory=list, description="Ranked list of potential duplicate matches")
    model_version: str = Field(..., alias="modelVersion", description="Duplicate model version identifier")
