"""
SmartDesk Ticket Classification Training Script
==============================================
Trains a classical scikit-learn Pipeline (TfidfVectorizer + LogisticRegression)
on the starter dataset and serializes the resulting model artifact.

NOTE ON DATASET & ACCURACY:
This is a starter demonstration corpus (90 examples across 9 categories) created
for reproducible classical ML pipeline validation. It is NOT a production-grade
training corpus. Model confidence represents the predicted softmax probability
from logistic regression, not a guarantee of real-world accuracy.
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

# Ensure app package is accessible
CURRENT_DIR = Path(__file__).resolve().parent
ROOT_DIR = CURRENT_DIR.parent
sys.path.insert(0, str(ROOT_DIR))

from app.preprocessing.text_cleaner import clean_text
from app.config import ALLOWED_CATEGORIES, MODEL_PATH, MODEL_VERSION

DATASET_PATH = CURRENT_DIR / "dataset.csv"

def train():
    print(f"[*] Loading training dataset from: {DATASET_PATH}")
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Dataset file not found at: {DATASET_PATH}")

    df = pd.read_csv(DATASET_PATH)
    print(f"[*] Total dataset records: {len(df)}")

    # Validate categories
    found_categories = set(df["category"].unique())
    print(f"[*] Categories present in dataset ({len(found_categories)}): {sorted(found_categories)}")

    missing_categories = set(ALLOWED_CATEGORIES) - found_categories
    if missing_categories:
        raise ValueError(f"Dataset is missing required categories: {missing_categories}")

    # Clean text inputs
    df["cleaned_text"] = df["text"].astype(str).apply(clean_text)

    X = df["cleaned_text"]
    y = df["category"]

    # Build Pipeline: TF-IDF Vectorizer + Logistic Regression
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(
            ngram_range=(1, 2),
            sublinear_tf=True,
            max_features=2500,
            stop_words="english",
        )),
        ("clf", LogisticRegression(
            C=2.0,
            max_iter=1000,
            random_state=42,
            class_weight="balanced",
            solver="lbfgs",
        )),
    ])

    print("[*] Training TF-IDF + LogisticRegression pipeline...")
    pipeline.fit(X, y)

    # Evaluate on training data (noting statistical limitation explicitly)
    y_pred = pipeline.predict(X)
    print("\n--- Training Set Classification Report (Demonstration Only) ---")
    print(classification_report(y, y_pred, zero_division=0))
    print("NOTE: Training evaluation shown for convergence verification only.")
    print("Due to the compact starter corpus size, statistically meaningful generalization metrics require production corpus.\n")

    # Serialize model
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    print(f"[OK] Successfully serialized trained model to: {MODEL_PATH}")
    print(f"[OK] Model Version: {MODEL_VERSION}")

if __name__ == "__main__":
    train()
