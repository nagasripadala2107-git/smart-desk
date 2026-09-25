"""
SmartDesk Customer Tone Detector
================================
Rule-based heuristic layer that analyzes input text and predicted sentiment
to classify customer communication tone into one of 7 categories:
CALM, FRUSTRATED, URGENT, ANGRY, SATISFIED, CONFUSED, NEUTRAL.

DISCLAIMER:
These heuristic rules are designed as transparent operational indicators for
customer support workflow demonstration. They are NOT scientifically validated
psychometric instruments.
"""

import re
from typing import Optional

ANGRY_INDICATORS = [
    r"\bfurious\b",
    r"\bunacceptable\b",
    r"\bterrible\b",
    r"\bhorrible\b",
    r"\bridiculous\b",
    r"\bworst\b",
    r"\bdemand\b",
    r"\bincompetent\b",
    r"\brip[\s-]?off\b",
    r"\blawsuit\b",
    r"\blegal\s+action\b",
    r"\bhate\b",
]

URGENT_INDICATORS = [
    r"\burgent\b",
    r"\bemergency\b",
    r"\basap\b",
    r"\bimmediately\b",
    r"\bproduction\s+down\b",
    r"\bcompletely\s+blocked\b",
    r"\bcritical\b",
    r"\bdeadline\b",
    r"\bcan'?t\s+access\b",
    r"\bcannot\s+access\b",
    r"\blockout\b",
    r"\blocked\s+out\b",
]

FRUSTRATED_INDICATORS = [
    r"\bstill\s+not\s+fixed\b",
    r"\bstill\s+waiting\b",
    r"\bwaiting\s+for\s+\w+\b",
    r"\bwaiting\b",
    r"\bdays\b",
    r"\bweeks\b",
    r"\bagain\b",
    r"\bnot\s+working\b",
    r"\bno\s+response\b",
    r"\bnobody\b",
    r"\bignored\b",
    r"\btried\s+multiple\s+times\b",
    r"\bfrustrated\b",
    r"\bfrustrating\b",
    r"\bdisappointed\b",
    r"\bbroken\b",
]

CONFUSED_INDICATORS = [
    r"\bdon'?t\s+understand\b",
    r"\bdo\s+not\s+understand\b",
    r"\bnot\s+sure\b",
    r"\bconfused\b",
    r"\bhow\s+do\s+i\b",
    r"\bhow\s+can\s+i\b",
    r"\bwhat\s+does\s+this\s+mean\b",
    r"\bwhich\s+one\b",
    r"\bwhere\s+do\s+i\b",
    r"\bwhy\s+is\b",
    r"\bwhy\s+does\b",
]

SATISFIED_INDICATORS = [
    r"\bthank\s+you\b",
    r"\bthanks\b",
    r"\bappreciate\b",
    r"\bhappy\b",
    r"\bsolved\b",
    r"\bresolved\b",
    r"\bworking\s+now\b",
    r"\bworks\s+great\b",
    r"\bflawlessly\b",
    r"\bawesome\b",
    r"\bperfect\b",
    r"\bgreat\s+job\b",
    r"\bwonderful\b",
    r"\bexcellent\b",
]

def detect_tone(text: str, sentiment: str) -> str:
    """
    Evaluates text and predicted sentiment against transparent heuristic rules.
    Returns one of: ANGRY, URGENT, FRUSTRATED, CONFUSED, SATISFIED, CALM, NEUTRAL.
    """
    if not text or not text.strip():
        return "NEUTRAL"

    lower_text = text.lower()

    # Excessive exclamation / aggressive capitalization test
    has_excessive_exclamation = bool(re.search(r"!{2,}", text))
    uppercase_words = [w for w in text.split() if len(w) > 2 and w.isupper()]
    has_heavy_caps = len(uppercase_words) >= 2

    # 1. ANGRY: Strong hostile signals, aggressive language, or excessive punctuation with negative sentiment
    for pattern in ANGRY_INDICATORS:
        if re.search(pattern, lower_text):
            return "ANGRY"

    if sentiment == "NEGATIVE" and (has_excessive_exclamation or has_heavy_caps):
        return "ANGRY"

    # 2. URGENT: Blocking, immediate-attention, production failure signals
    for pattern in URGENT_INDICATORS:
        if re.search(pattern, lower_text):
            return "URGENT"

    # 3. FRUSTRATED: Repetitive failure, delays, unaddressed issues
    if sentiment == "NEGATIVE":
        for pattern in FRUSTRATED_INDICATORS:
            if re.search(pattern, lower_text):
                return "FRUSTRATED"
        # If negative without explicit anger/urgency, default to FRUSTRATED
        return "FRUSTRATED"

    # 4. SATISFIED: Gratitude, praise, problem-resolved confirmations
    if sentiment == "POSITIVE":
        for pattern in SATISFIED_INDICATORS:
            if re.search(pattern, lower_text):
                return "SATISFIED"
        return "SATISFIED"

    # 5. CONFUSED: Uncertainty, questions, or clarification needs
    for pattern in CONFUSED_INDICATORS:
        if re.search(pattern, lower_text):
            return "CONFUSED"

    # 6. CALM: Factual, polite, non-emotional statements
    if "?" not in text and not has_excessive_exclamation:
        # Check word length: standard factual request
        words = lower_text.split()
        if len(words) >= 3:
            return "CALM"

    # 7. NEUTRAL: Fallback default
    return "NEUTRAL"
