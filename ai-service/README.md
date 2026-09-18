# SmartDesk AI Service (Python)

This directory will contain the lightweight, dedicated AI/ML service for ticket categorization and triage inference.

## Technology Stack
- **Framework**: FastAPI / Uvicorn
- **Language**: Python 3.14+
- **Machine Learning**: scikit-learn (TF-IDF Vectorizer + Logistic Regression)
- **Role**:
  - Receives ticket subject and description from the Java backend
  - Returns predicted category, confidence score, and model metadata
  - Independent microservice keeping ML workload decoupled from core business operations

*Note: Initialized as placeholder during Phase 1. Virtual environment and model service will be set up in subsequent phases.*
