"""
SmartDesk Ticket Sentiment & Tone Analysis Training Script
==========================================================
Trains a classical scikit-learn Pipeline (TfidfVectorizer + LogisticRegression)
for 3-class sentiment prediction (POSITIVE, NEUTRAL, NEGATIVE).

NOTE ON DATASET & ACCURACY:
This is a starter demonstration corpus (90 examples across 3 sentiment classes)
created for reproducible classical ML validation in a student/client project.
Model confidence represents softmax predicted probability, not an absolute
guarantee of human emotional state.
"""

import os
import sys
from pathlib import Path
import pandas as pd
import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import classification_report

CURRENT_DIR = Path(__file__).resolve().parent
ROOT_DIR = CURRENT_DIR.parent
sys.path.insert(0, str(ROOT_DIR))

from app.preprocessing.text_cleaner import clean_text
from app.config import SENTIMENT_MODEL_PATH, SENTIMENT_MODEL_VERSION, ALLOWED_SENTIMENTS

DATASET_PATH = CURRENT_DIR / "sentiment_dataset.csv"

def train():
    print(f"[*] Loading sentiment training dataset from: {DATASET_PATH}")
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Dataset file not found at: {DATASET_PATH}")

    df = pd.read_csv(DATASET_PATH)
    print(f"[*] Total dataset records: {len(df)}")

    found_classes = set(df["sentiment"].unique())
    print(f"[*] Sentiments present in dataset ({len(found_classes)}): {sorted(found_classes)}")

    missing_classes = set(ALLOWED_SENTIMENTS) - found_classes
    if missing_classes:
        raise ValueError(f"Dataset is missing required sentiments: {missing_classes}")

    # Clean text inputs
    df["cleaned_text"] = df["text"].astype(str).apply(clean_text)

    X = df["cleaned_text"]
    y = df["sentiment"]

    # Build Pipeline: TF-IDF Vectorizer + Logistic Regression
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(
            ngram_range=(1, 2),
            sublinear_tf=True,
            max_features=1500,
            stop_words="english",
        )),
        ("clf", LogisticRegression(
            C=1.5,
            max_iter=1000,
            random_state=42,
            class_weight="balanced",
            solver="lbfgs",
        )),
    ])

    print("[*] Training TF-IDF + LogisticRegression sentiment pipeline...")
    pipeline.fit(X, y)

    # Evaluate on training data
    y_pred = pipeline.predict(X)
    print("\n--- Training Set Classification Report (Demonstration Only) ---")
    print(classification_report(y, y_pred, zero_division=0))

    # Serialize model
    SENTIMENT_MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, SENTIMENT_MODEL_PATH)
    print(f"[OK] Successfully serialized trained sentiment model to: {SENTIMENT_MODEL_PATH}")
    print(f"[OK] Model Version: {SENTIMENT_MODEL_VERSION}")

if __name__ == "__main__":
    train()
