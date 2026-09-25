import logging
from typing import List, Tuple
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.config import (
    DUPLICATE_MODEL_VERSION,
    DUPLICATE_THRESHOLD,
    DUPLICATE_MAX_MATCHES,
)
from app.preprocessing.text_cleaner import clean_text

logger = logging.getLogger(__name__)

class DuplicateDetector:
    """
    Lightweight local similarity detector using TF-IDF vectorization
    and cosine similarity to identify potential duplicate tickets.
    """

    def __init__(
        self,
        model_version: str = DUPLICATE_MODEL_VERSION,
        threshold: float = DUPLICATE_THRESHOLD,
        max_matches: int = DUPLICATE_MAX_MATCHES,
    ):
        self.model_version = model_version
        self.threshold = threshold
        self.max_matches = max_matches

    @property
    def is_loaded(self) -> bool:
        """
        Duplicate detector operates on dynamic TF-IDF vectorization and is always ready.
        """
        return True

    def find_duplicates(
        self,
        target_text: str,
        candidates: List[Tuple[str, str]],
        threshold: float = None,
        max_matches: int = None,
    ) -> List[Tuple[str, float]]:
        """
        Calculates cosine similarity between target_text and a list of candidates.

        Args:
            target_text: Subject and description of the newly created ticket.
            candidates: List of tuples (ticket_id, text).
            threshold: Similarity cutoff (defaults to self.threshold).
            max_matches: Maximum number of matches to return (defaults to self.max_matches).

        Returns:
            List of (ticket_id, similarity_score) sorted descending by similarity.
        """
        if not candidates or not target_text or not target_text.strip():
            return []

        active_threshold = threshold if threshold is not None else self.threshold
        active_max_matches = max_matches if max_matches is not None else self.max_matches

        cleaned_target = clean_text(target_text)
        if not cleaned_target:
            return []

        cleaned_candidates = []
        candidate_ids = []
        for ticket_id, text in candidates:
            cleaned = clean_text(text)
            if cleaned:
                cleaned_candidates.append(cleaned)
                candidate_ids.append(ticket_id)

        if not cleaned_candidates:
            return []

        try:
            # Fit TF-IDF on corpus consisting of target text + candidate texts
            corpus = [cleaned_target] + cleaned_candidates
            vectorizer = TfidfVectorizer(
                stop_words="english",
                ngram_range=(1, 1),
                min_df=1,
                lowercase=True,
            )
            tfidf_matrix = vectorizer.fit_transform(corpus)

            target_vec = tfidf_matrix[0:1]
            candidate_vecs = tfidf_matrix[1:]

            # Compute cosine similarities between target and all candidates
            similarities = cosine_similarity(target_vec, candidate_vecs).flatten()

            # Filter, sort descending, and limit
            matches: List[Tuple[str, float]] = []
            for idx, sim in enumerate(similarities):
                score = float(sim)
                # Ensure bounded within [0.0, 1.0]
                bounded_score = max(0.0, min(1.0, round(score, 4)))
                if bounded_score >= active_threshold:
                    matches.append((candidate_ids[idx], bounded_score))

            # Sort descending by similarity score
            matches.sort(key=lambda m: m[1], reverse=True)

            # Cap at max_matches
            return matches[:active_max_matches]

        except Exception as e:
            logger.error("Error during duplicate similarity calculation: %s", str(e), exc_info=True)
            raise

# Global singleton instance
duplicate_detector = DuplicateDetector()
