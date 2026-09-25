from app.models.duplicate_detector import duplicate_detector
from app.schemas.duplicate_schema import (
    DuplicateRequest,
    DuplicateResponse,
    DuplicateMatch,
)
from app.config import DUPLICATE_MODEL_VERSION

class DuplicateService:
    @staticmethod
    def analyze(request: DuplicateRequest) -> DuplicateResponse:
        candidates_tuples = [(c.ticket_id, c.text) for c in request.candidates]
        raw_matches = duplicate_detector.find_duplicates(
            target_text=request.text,
            candidates=candidates_tuples,
        )

        matches = [
            DuplicateMatch(ticketId=ticket_id, similarity=score)
            for ticket_id, score in raw_matches
        ]

        return DuplicateResponse(
            matches=matches,
            modelVersion=DUPLICATE_MODEL_VERSION,
        )

duplicate_service = DuplicateService()
