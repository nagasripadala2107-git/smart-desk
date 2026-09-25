# SmartDesk AI Ticket Classification Service (Phase 5)

Dedicated lightweight Python FastAPI microservice responsible exclusively for text-based support ticket category classification.

---

## 1. Overview & Architectural Role
- **Service Name**: SmartDesk Ticket Classifier
- **Model Version**: `ticket-classifier-v1`
- **Framework**: FastAPI + Uvicorn
- **Machine Learning**: `scikit-learn` Pipeline (`TfidfVectorizer` + `LogisticRegression`)
- **Port**: `8000` (Default)
- **Role & Boundaries**:
  - Receives ticket `subject` and `description` from the Java backend.
  - Returns predicted `category`, `confidence` score, and `model_version`.
  - **Business Authority**: Spring Boot remains the sole authority. The Python service never assigns teams, agents, priorities, statuses, or escalations.

---

## 2. Confidence Definition & Limitations

> [!IMPORTANT]
> **What Confidence Score Means:**
> In this service, `confidence` represents the **model's predicted probability** output by the logistic regression softmax/predict_proba function for the selected category.
>
> **What Confidence Score Does NOT Mean:**
> `confidence` is **NOT a guaranteed real-world accuracy score**. A confidence score of `0.92` simply means the TF-IDF feature space aligned closely with training examples for that category, not that the classification is 92% guaranteed to be objectively correct in production.

### Starter Dataset & Limitations:
- **Dataset**: `training/dataset.csv`
- **Dataset Size**: 90 curated examples (10 balanced samples per category across all 9 supported categories).
- **Source**: Synthetic demo/starter dataset designed for deterministic local testing and integration development.
- **Statistical Significance**: Because the starter dataset is small (90 samples), train/test split evaluation metrics are for pipeline integrity verification only and **do not constitute a production accuracy claim**.

---

## 3. Supported Categories (Strictly 9)

The classification output is constrained strictly to the 9 SmartDesk system categories:
1. `BILLING`
2. `TECHNICAL`
3. `ACCOUNT`
4. `REFUND`
5. `SECURITY`
6. `SUBSCRIPTION`
7. `BUG`
8. `FEATURE_REQUEST`
9. `OTHER`

---

## 4. Local Setup & Virtual Environment

```bash
cd ai-service

# Create virtual environment
python -m venv .venv

# Activate on Windows
.\.venv\Scripts\Activate.ps1

# Install dependencies
pip install -r requirements.txt
```

---

## 5. Model Training Command

Train the model and serialize to `models/ticket_classifier.pkl`:

```bash
cd ai-service
.\.venv\Scripts\python.exe training/train.py
```

Training parameters:
- Algorithm: `TfidfVectorizer(max_features=2500, ngram_range=(1, 2), stop_words='english')` + `LogisticRegression(C=2.0, max_iter=1000, random_state=42)`
- Output artifact: `models/ticket_classifier.pkl` (loaded once into memory at FastAPI lifespan startup, never retrained per request).

---

## 6. Running the Service

### A. In Docker Compose (Recommended)
The AI service is orchestrated via Docker Compose:
```bash
# From repository root
docker compose up -d ai-service
```
- **Port Security**: The service binds to port `8000` internally within `smartdesk-network`. It is **NOT** exposed publicly to the host machine.
- **Internal Access**: Only internal containers (such as `smartdesk-backend`) can invoke inference via `http://ai-service:8000/api/v1/classify`.
- **Health Check**: Docker monitors container health automatically via `http://localhost:8000/health`.

### B. Running Locally (Development)
```bash
cd ai-service
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Endpoints:
- `GET /health`: Liveness & model readiness check (returns `{"status": "UP", "model_loaded": true}`).
- `POST /api/v1/classify`: Classify ticket subject and description into one of 9 domain categories.
- `GET /api/v1/model-info` (or `/api/v1/model/info`): Model metadata, algorithm (`TF-IDF + Logistic Regression`), and category catalog.

#### Example Request:
```bash
curl -X POST http://127.0.0.1:8000/api/v1/classify \
  -H "Content-Type: application/json" \
  -d '{"subject": "Payment charged twice", "description": "My card was charged two times on invoice INV-1002."}'
```

#### Example Response:
```json
{
  "category": "BILLING",
  "confidence": 0.3778,
  "model_version": "ticket-classifier-v1"
}
```

---

## 7. Running Tests

```bash
cd ai-service
.\.venv\Scripts\pytest -v
```

Test coverage:
- Health check & model info endpoints
- Multi-category classification
- Strict category validation (must be in 9 allowed categories)
- Empty / blank input validation (HTTP 422)
- Missing model startup failure handling (HTTP 503)

---

## 8. Java Integration & Graceful Degradation

- **Timeout**: Default 2000 ms (`app.ai.timeout-ms`), configurable via `AI_TIMEOUT_MS`.
- **Base URL**: Default `http://localhost:8000` (`app.ai.service-url`), configurable via `AI_SERVICE_URL`.
- **Graceful Degradation Guarantee**:
  If the Python service is offline, times out, throws HTTP 5xx, or returns unparseable content:
  1. The Java backend catches all exceptions.
  2. A concise warning is logged without printing raw customer issue text.
  3. `ai_category = null`, `ai_confidence = null` are assigned.
  4. Ticket creation **NEVER fails** and proceeds immediately to deterministic Java routing.
- **Explicit Category Preservation**:
  If the customer explicitly selects a category in `CreateTicketRequest`, the Java backend preserves the customer's selection. AI still analyzes the text, but the explicit selection is not overwritten.
