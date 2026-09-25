import re

def clean_text(text: str) -> str:
    """
    Normalizes input text:
    - Trims leading/trailing whitespace
    - Converts to lowercase
    - Normalizes internal whitespace and newlines into single spaces
    """
    if not text:
        return ""
    text = text.strip().lower()
    text = re.sub(r"\s+", " ", text)
    return text

def prepare_input(subject: str, description: str) -> str:
    """
    Combines subject and description for text classification.
    E.g. subject + " " + description.
    """
    clean_sub = clean_text(subject)
    clean_desc = clean_text(description)
    if clean_sub and clean_desc:
        return f"{clean_sub} {clean_desc}"
    return clean_sub or clean_desc
