# SmartDesk 🚀
### AI-Powered Customer Support & Intelligent Ticket Routing Platform

SmartDesk is an enterprise-grade customer support platform designed to streamline ticket ingestion, automate classification using local machine learning, balance support queues across teams, enforce SLAs, and maintain comprehensive auditability.

---

## 1. Project Overview

SmartDesk addresses modern IT and customer service bottlenecks by unifying multi-channel ticket ingestion with localized, low-latency machine learning inference. Built on a robust polyglot architecture:
- **Java Spring Boot** acts as the primary business backend, managing persistence, state transitions, security, and transaction boundaries.
- **Python FastAPI** serves as a specialized AI microservice running deterministic scikit-learn classification, sentiment analysis, tone detection, and duplicate ticket identification.
- **Next.js (React 19)** provides a modern, responsive web frontend with dedicated workspaces for Customers, Agents, and Administrators.
- **PostgreSQL 17** provides an ACID-compliant relational store with 17 normalized tables and full relational integrity.
- **Nginx** acts as the production reverse proxy, handling SSL/TLS termination, rate limiting, and request routing across internal micro-segmented Docker networks.

---

## 2. Problem Statement

Customer support desks often struggle with:
1. **Manual Triage Delays**: High volume of incoming tickets leads to human dispatch delays, inconsistent categorization, and delayed initial response.
2. **Support Agent Burnout**: Repetitive queries and unidentified duplicate tickets inflate queue backlogs.
3. **Lack of Emotional Context**: Critical client frustration or urgent escalations are easily overlooked in flat text queues.
4. **Security & Tenant Isolation Risks**: Direct object reference vulnerabilities (IDOR) and cross-tenant data leakage in multi-customer platforms.
5. **Costly External LLM Dependencies**: High cloud token costs, unpredictable latencies, and third-party data privacy compliance risks.

SmartDesk resolves these challenges with an on-premise, secure, and privacy-first architecture using local ML models that require zero external API calls.

---

## 3. Features

- **Sequential Ticket Numbering**: Formatted `SD-YYYY-XXXXXX` tracking.
- **Local Machine Learning Ingestion**: Automatic category prediction and confidence scoring at ticket submission.
- **Sentiment & Tone Profiling**: Real-time emotional analysis (Positive, Neutral, Negative) and lexical tone detection (Frustrated, Urgent, Polite).
- **Duplicate Ticket Detection**: Cosine similarity matching against open tickets to prevent redundant effort.
- **Workload-Balanced Routing**: Rule-based and capacity-aware ticket dispatching to specialist teams.
- **Controlled Ticket Lifecycle**: Strict state machine governing transitions from creation to closure with transition reason logging.
- **Multi-Tier Escalation**: Tiered reassignment with automated SLA countdown timers.
- **Customer 360 View**: Instant historical ticket and plan context for support agents.
- **Executive Analytics**: Live operational dashboards tracking resolution times, SLA compliance, and category distribution.
- **Enterprise Security**: HMAC-SHA256 JWT authentication, BCrypt password hashing, sliding-window rate limiting, and strict customer IDOR protection.

---

## 4. Architecture

SmartDesk employs a micro-segmented production architecture with strict physical separation between public ingress and internal data layers:

```
Browser (Customer / Agent / Admin)
   ↓ HTTPS (Port 443) / HTTP (Port 80 -> 301 Redirect)
[ NGINX REVERSE PROXY ] (smartdesk-nginx-prod)
   ├── /               →  Next.js Frontend (:3000, internal)
   ├── /api/v1/        →  Spring Boot Backend (:8080, internal)
   └── /health         →  Actuator Probe (:8080/actuator/health, internal)

[ INTERNAL DOCKER NETWORKS ]
   ├── frontend-net: Nginx, Frontend, Backend
   └── backend-net:  Backend, PostgreSQL, AI Service, Backup Service

Spring Boot Backend
   ├── JDBC / Transactions  →  PostgreSQL 17 (:5432, internal only)
   ├── REST / JSON (DNS)    →  Python FastAPI AI Service (:8000, internal only)
   └── Backup Service       →  Automated pg_dump to /backups volume
```

### Network Isolation Boundaries
- **Public**: Nginx (`80`, `443`).
- **Internal Only**: PostgreSQL (`5432`), Spring Boot (`8080`), FastAPI AI (`8000`), Next.js (`3000`), Backup daemon. No internal services bind to host interfaces in production.

---

## 5. Technology Stack

| Layer | Technology | Key Details |
| :--- | :--- | :--- |
| **Ingress Proxy** | Nginx 1.27 Alpine | TLS termination, HTTP-to-HTTPS redirect, security headers |
| **Frontend** | Next.js 16.3.5 / React 19 | TypeScript, Tailwind CSS, shadcn/ui, Recharts |
| **Business Backend**| Java 26 / Spring Boot 4.1.1 | Spring Security, Spring Data JPA, Hibernate, Bucket4j |
| **AI Inference** | Python 3.14 / FastAPI / Uvicorn | Scikit-learn (TF-IDF + Logistic Regression), Pydantic |
| **Database** | PostgreSQL 17 Alpine | ACID relational store, 17 tables, UUID keys, JSONB audit |
| **Backup** | Alpine Linux / PostgreSQL Client | Daily automated `pg_dump`, SHA-256 validation, 14-day retention |
| **Containerization**| Docker / Docker Compose | Multi-stage builds, non-root users, resource quotas |

---

## 6. Repository Structure

```text
smartdesk/
├── frontend/             # Next.js web application (Customer, Agent & Admin portals)
├── backend-java/         # Java Spring Boot enterprise core backend (Business rules & routing)
├── ai-service/           # Python FastAPI machine learning ticket classification microservice
├── database/             # Relational schema DDL, versioned migrations, and seed datasets
├── docker/               # Container configurations, Nginx configs, backup scripts
│   ├── nginx/            # Production and staging Nginx configuration templates
│   └── backup/           # Automated database backup scripts and crontab definitions
├── docs/                 # Architecture, deployment, backup runbooks, and test reports
├── docker-compose.yml    # Local development multi-container orchestration
├── docker-compose.prod.yml # Production multi-container orchestration
├── .env.example          # Safe template for host development
├── .env.docker.example   # Safe template for local Docker development
├── .env.prod.example     # Safe template for production deployment
└── README.md             # Project master overview and quickstart guide
```

---

## 7. User Roles & Permissions

SmartDesk implements strict Role-Based Access Control (RBAC):

1. **`ROLE_CUSTOMER`**:
   - Create new support tickets with attachments/details.
   - View only tickets submitted by their customer profile (strict tenant boundary).
   - Post replies to open tickets.
   - *Restricted*: Cannot view internal staff notes, sentiment analysis, duplicate matches, or agent queues.
2. **`ROLE_AGENT`**:
   - Access assigned team queues and unassigned backlogs.
   - Update ticket status, reassign tickets, and add internal notes.
   - Inspect AI sentiment scores, tone indicators, and duplicate candidate lists.
   - Review Customer 360 profile summaries.
3. **`ROLE_ADMIN`**:
   - Full system administration, user management, and team assignment.
   - Access executive operational analytics (`/api/v1/analytics/*`).
   - Configure SLA policies and review system audit timelines.

---

## 8. Ticket Lifecycle State Machine

Tickets progress through a formal, deterministic state machine:

```
[ CREATED ]
    ↓ (Automatic AI triage & team assignment)
[ ASSIGNED ]
    ↓ (Agent begins active work)
[ IN_PROGRESS ] ⇄ [ PENDING_CUSTOMER ] (Waiting on customer input)
    ↓ (Optional manual or automated SLA trigger)
[ ESCALATED ] ⇄ [ IN_PROGRESS ]
    ↓ (Resolution confirmed by agent)
[ RESOLVED ]
    ↓ (Customer confirmation or 48-hour timeout)
[ CLOSED ]
```

All status changes record transition timestamps, acting user ID, and transition reason in `ticket_events`.

---

## 9. AI Features & Machine Learning Pipeline

SmartDesk runs **100% local, on-premise machine learning** with zero reliance on external paid LLM APIs:

- **Category Classification**: TF-IDF feature extraction with a multi-class Logistic Regression classifier trained across 9 operational domains (Billing, Network, Hardware, Access, Software, Security, General).
- **Sentiment Scoring**: TF-IDF and Logistic Regression classifying customer communication as `POSITIVE`, `NEUTRAL`, or `NEGATIVE`.
- **Tone Detection**: Rule-based lexical analyzer identifying emotional markers (`FRUSTRATED`, `URGENT`, `POLITE`, `FORMAL`).
- **Duplicate Ticket Detection**: Cosine similarity analysis against vectorized open ticket titles and descriptions within the same category (similarity threshold: 0.75).

### AI Advisory Boundaries & Failure Behavior
- **Advisory Role**: AI predictions are strictly advisory. The AI service **never** automatically closes, merges, deletes, or escalates tickets.
- **Graceful Degradation**: If the AI microservice is stopped or unreachable:
  - Customer ticket creation continues seamlessly without error.
  - The ticket is assigned to the fallback general queue.
  - Category and sentiment fields default safely to `null`.
  - Zero internal stack traces or Python error details are exposed to the client.

---

## 10. Intelligent Ticket Routing Engine

The routing engine dispatches newly ingested tickets based on:
1. **Predicted Category**: Matches category domain against team specializations.
2. **Customer Plan Tier**: Enterprise customers receive priority routing over Standard and Free tiers.
3. **Agent Workload Capacity**: Balances assignments across online agents based on active open ticket counts.

---

## 11. Multi-Tier Escalation Framework

When tickets breach target thresholds or require senior oversight:
- **Manual Escalation**: Agents can escalate tickets to Tier 2 (Senior Support) or Tier 3 (Management) with mandatory escalation reason logging.
- **Automated Escalation**: Background scheduler evaluates tickets nearing SLA breach, automatically elevating priority or notifying team leads.

---

## 12. Service Level Agreement (SLA) Management

SLA policies define maximum response and resolution times by priority:

| Priority | Target First Response | Target Resolution | Escalation Threshold |
| :--- | :--- | :--- | :--- |
| **URGENT** | 15 Minutes | 2 Hours | 90 Minutes |
| **HIGH** | 1 Hour | 8 Hours | 6 Hours |
| **MEDIUM** | 4 Hours | 24 Hours | 18 Hours |
| **LOW** | 8 Hours | 48 Hours | 36 Hours |

---

## 13. Operational Analytics Dashboard

Administrators have access to real-time aggregated metrics:
- **Throughput Overview**: Ingestion rates, active queues, and average resolution times.
- **SLA Compliance Tracking**: Visual compliance percentages and breach counts.
- **Sentiment Trends**: Longitudinal sentiment health across customer accounts.
- **Team Performance**: Queue volume, resolution efficiency, and agent capacity.

---

## 14. Security & Compliance Architecture

- **Authentication**: Stateless HMAC-SHA256 JWT tokens with active user status validation on every request.
- **Password Protection**: BCrypt hashing with cost factor 12.
- **Rate Limiting**: Sliding-window rate limiting (Bucket4j) enforcing 30 requests/minute per client IP on authentication routes (HTTP 429 on 31st request).
- **IDOR Protection**: Customers cannot access, query, or modify tickets belonging to other customer profiles.
- **Security Headers**: Enforced across Nginx and Spring Security (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`).
- **Data Protection**: Sensitive customer profile endpoints return strict `Cache-Control: no-cache, no-store`.

---

## 15. Docker Setup (Local Development)

To start the full development stack on your local machine:

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd smartdesk
   ```
2. Create local environment configuration:
   ```bash
   cp .env.docker.example .env
   ```
3. Start all containers:
   ```bash
   docker compose up -d
   ```
4. Verify container health:
   ```bash
   docker compose ps
   ```
5. Access the application:
   - Frontend: `http://localhost:3000`
   - Backend API: `http://localhost:8080/api/v1`
   - Backend Health: `http://localhost:8080/actuator/health`

---

## 16. Development Setup (Host Execution)

If developing outside Docker:
- **Backend**: `cd backend-java && ./mvnw spring-boot:run` (Requires Java 21+ and local PostgreSQL).
- **AI Microservice**: `cd ai-service && uvicorn app.main:app --port 8000 --reload` (Requires Python 3.11+).
- **Frontend**: `cd frontend && npm run dev` (Requires Node.js 20+).

---

## 17. Production Deployment

Production deployment is fully containerized and driven by `docker-compose.prod.yml`:

```bash
# 1. Prepare environment configuration
cp .env.prod.example .env.prod
chmod 600 .env.prod

# 2. Place trusted TLS certificates into ./certs/
# fullchain.pem and privkey.pem

# 3. Build production images
docker compose -f docker-compose.prod.yml build

# 4. Start production stack
docker compose -f docker-compose.prod.yml up -d

# 5. Verify health
docker compose -f docker-compose.prod.yml ps
curl -k https://<domain>/health
```

*For complete end-to-end instructions, see the [Production Deployment Runbook](file:///c:/Users/prasanna/ticket/docs/production-deployment.md).*

---

## 18. Testing & Verification

SmartDesk maintains extensive automated test suites:
- **Backend (Java)**: 96 tests run (94 passed, 2 skipped offline tests, 0 failures).
- **AI Microservice (Python)**: 44 tests run (44 passed, 100%).
- **Frontend (Jest)**: 50 tests run (50 passed, 100%).
- **Frontend Build**: 24/24 static and dynamic routes compiled cleanly.
- **Staging Verification**: All 8 end-to-end staging validation scenarios passed.

*For complete details, see the [Final Test Report](file:///c:/Users/prasanna/ticket/docs/final-test-report.md).*

---

## 19. Backup & Disaster Recovery

- **Automated Routine**: Nightly `pg_dump` runs at 02:00 UTC inside the `smartdesk-backup-prod` container.
- **Integrity**: Every backup produces a companion `.sha256` checksum.
- **Retention**: Automated pruning of archives older than 14 days.
- **Sandbox Restoration**: Documented zero-disruption restore testing runbook in `docs/database-backup-restore.md`.

---

## 20. Environment Variables Guide

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `POSTGRES_DB` | Production database name | `smartdesk` |
| `POSTGRES_USER` | Production database user | `postgres` |
| `POSTGRES_PASSWORD` | Cryptographically random DB password | *Keep Secret* |
| `JWT_SECRET` | 512-bit HMAC secret for token signing | *Keep Secret* |
| `DOMAIN_NAME` | Fully Qualified Domain Name (FQDN) | `support.example.com` |
| `AI_SERVICE_URL` | Internal Docker URL for AI microservice | `http://ai-service:8000` |
| `BACKUP_RETENTION_DAYS` | Daily backup archive retention limit | `14` |

---

## 21. Limitations

1. **Localized Machine Learning**: Models use TF-IDF and Logistic Regression. They provide fast, reliable domain categorization but do not generate free-form conversational text.
2. **No Pre-Packaged Cloud Infrastructure**: Deployment is containerized and host-agnostic; cloud infrastructure provisioning (Terraform, AWS ECS/EKS) is not bundled.
3. **Manual Certificate Acquisition**: Production HTTPS requires the administrator to acquire valid TLS certificates via ACME/Let's Encrypt for their specific FQDN.
4. **Local Backup Storage**: Automated backups reside on host persistent Docker volumes; off-host S3/blob replication requires manual external synchronization.
5. **No Native SMS/Email Gateway**: Ticket communications occur via the web application interface; email/SMS webhooks are not implemented in the current release.

---

## 22. Future Roadmap

- **AI Enhancements**:
  - LLM-assisted suggested replies for support agents with Retrieval-Augmented Generation (RAG).
  - Expanded multilingual training datasets for non-English ticket triage.
- **Infrastructure & Reliability**:
  - Off-host backup replication to AWS S3 or Google Cloud Storage.
  - Centralized OpenTelemetry tracing and Prometheus/Grafana metrics dashboards.
  - Multi-node high availability database clustering with read replicas.
- **Product Features**:
  - Inbound email ingestion via SendGrid/Mailgun webhooks.
  - File attachment support with virus scanning and S3 object storage.
  - Real-time WebSocket push notifications for active ticket conversations.
