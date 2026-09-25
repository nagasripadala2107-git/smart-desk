# SmartDesk Production Architecture Specification

This document details the multi-tier containerized production architecture for **SmartDesk**, an AI-powered customer support and intelligent ticket routing platform.

---

## 1. High-Level Ingress & Service Topology

```text
Browser Client (HTTPS)
       │
       │ TCP 443 (TLS Termination) / TCP 80 (HTTP 301 Redirect)
       ▼
+------------------------------------------------------------------------------------+
| PUBLIC BOUNDARY: Nginx Ingress Reverse Proxy (smartdesk-prod-nginx)               |
| - Listens on external host ports :80 and :443                                      |
| - Edge Rate Limiting: 30 requests/minute/IP on /api/v1/auth/                       |
| - Next.js Static Asset Caching: /_next/static/ (365d immutable)                   |
| - Security Headers: X-Content-Type-Options, X-Frame-Options, Referrer-Policy       |
+------------------------------------------------------------------------------------+
       │                                │                                │
       │ / (Default Route)              │ /api/v1/ (REST API)            │ /health (Probe)
       ▼                                ▼                                ▼
+-----------------------------+  +-----------------------------+  +-------------------+
| Next.js Frontend            |  | Java Spring Boot Backend    |  | Backend Actuator  |
| (smartdesk-prod-frontend)   |  | (smartdesk-prod-backend)    |  | (/actuator/health)|
| - Standalone Node 26        |  | - Spring Boot 4.1.1 (Java 26|  +-------------------+
| - Port 3000 (INTERNAL ONLY) |  | - Profile: 'prod'           |
| - Non-root: nextjs (1001)   |  | - Port 8080 (INTERNAL ONLY) |
+-----------------------------+  | - Non-root: appuser (1001)  |
                                 +-----------------------------+
                                                │
                 ┌──────────────────────────────┴──────────────────────────────┐
                 │ JDBC (HikariCP Pool: 5-20)                                  │ HTTP (Internal DNS)
                 ▼                                                             ▼
+---------------------------------------------+               +-------------------------------+
| Relational Database                         |               | AI Classification Service     |
| (smartdesk-prod-postgres)                   |               | (smartdesk-prod-ai)           |
| - PostgreSQL 17                             |               | - Python 3.14 FastAPI         |
| - Port 5432 (INTERNAL ONLY)                 |               | - 2 Uvicorn Workers           |
| - Volume: smartdesk_postgres_data           |               | - Resource Bound: 1 CPU / 1GB |
+---------------------------------------------+               | - Port 8000 (INTERNAL ONLY)   |
                 ▲                                            | - Non-root: appuser (1001)    |
                 │ pg_dump (Nightly Cron)                     +-------------------------------+
+---------------------------------------------+
| Automated Backup Service                    |
| (smartdesk-prod-backup)                     |
| - PostgreSQL 17 Alpine Client               |
| - Gzip compressed, 14-day retention         |
| - Volume: smartdesk_prod_backups            |
+---------------------------------------------+
```

---

## 2. Network Boundary Classification

### Public Tier (Exposed to External Network)
| Component | Container Name | Published Host Ports | Description |
| :--- | :--- | :--- | :--- |
| **Nginx Ingress** | `smartdesk-prod-nginx` | `80:80`, `443:443` | Sole public entry point. Terminates TLS, enforces edge rate limiting, serves static assets, and reverse-proxies to internal application tiers. |

### Internal Tier (Completely Isolated from External Network)
| Component | Container Name | Internal Port | Host Exposure | Security Isolation |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend** | `smartdesk-prod-frontend` | `3000` | **NONE** | Reachable only by Nginx via `http://frontend:3000`. |
| **Backend** | `smartdesk-prod-backend` | `8080` | **NONE** | Reachable only by Nginx via `http://backend:8080`. |
| **AI Service** | `smartdesk-prod-ai` | `8000` | **NONE** | Reachable only by Backend via `http://ai-service:8000`. |
| **Database** | `smartdesk-prod-postgres` | `5432` | **NONE** | Reachable only by Backend and Backup service via `postgres:5432`. |
| **Backup** | `smartdesk-prod-backup` | N/A | **NONE** | Internal background worker scheduled via cron daemon. |

---

## 3. Component Architecture & Responsibilities

### 1. Nginx Reverse Proxy (`smartdesk-prod-nginx`)
- **Base Image**: `nginx:1.27-alpine`
- **TLS Termination**: Modern TLS 1.2 and 1.3 with high-security ciphers (`HIGH:!aNULL:!MD5:!3DES`).
- **HTTP Redirection**: Listens on port 80 and issues permanent 301 redirects to `https://$host$request_uri`.
- **Unified Origin Routing**:
  - `/` routes to Next.js (`frontend:3000`).
  - `/api/v1/` routes to Spring Boot (`backend:8080`).
  - `/health` routes to sanitized Actuator health check (`backend:8080/actuator/health`). All other `/actuator/**` endpoints are blocked.
- **Edge Rate Limiting**: `limit_req_zone` allocated with 10MB memory zone at 30 requests/minute per client IP for `/api/v1/auth/` routes.
- **Performance Optimization**: Gzip compression for text, JSON, CSS, JS; 365-day immutable caching on `/_next/static/`.

### 2. Next.js Frontend (`smartdesk-prod-frontend`)
- **Runtime**: Node.js 26 on Alpine Linux.
- **Build Mode**: Next.js Standalone build (`output: 'standalone'`).
- **Client API Path**: Built with `NEXT_PUBLIC_API_URL=/api/v1` relative path. Zero hardcoded references to `localhost:8080`.
- **Process Security**: Dedicated non-root user `nextjs` (UID 1001), non-root group `nodejs` (GID 1001).
- **Health Monitoring**: HTTP probe targeting `http://127.0.0.1:3000/`.

### 3. Java Spring Boot Backend (`smartdesk-prod-backend`)
- **Runtime**: Eclipse Temurin 26 JRE on Ubuntu base.
- **Framework**: Spring Boot 4.1.1 with Spring Security 7.1.1 and Spring Data JPA.
- **Active Profile**: `SPRING_PROFILES_ACTIVE=prod`.
- **Database Connection Management**: HikariCP connection pool (`SmartDeskHikariPool-Prod`) with 20 maximum connections, 5 minimum idle, 20s connection timeout, 30m maximum lifetime.
- **JPA Safety**: `hibernate.ddl-auto: validate` prevents schema modifications at runtime.
- **Information Masking**: `server.error.include-stacktrace: never` masks internal exceptions from API callers.
- **Logging**: Production log levels set to INFO for application code, WARN for Hibernate and SQL statements.
- **Process Security**: Dedicated non-root user `appuser` (UID 1001), non-root group `appgroup` (GID 999).
- **Health Check**: Native standalone Java utility (`HealthCheck.class`) probing `/actuator/health`.

### 4. Python FastAPI AI Service (`smartdesk-prod-ai`)
- **Runtime**: Python 3.14-slim.
- **Concurrency**: Uvicorn server running 2 worker processes.
- **Resource Boundaries**: Strict Docker container limits enforced (`cpus: "1.0"`, `memory: 1024M`).
- **Machine Learning Architecture**:
  - **Category Classification**: Scikit-learn `TfidfVectorizer` paired with `LogisticRegression` pipeline. Classifies tickets into 9 predefined domains (Billing, Technical, Account, Refund, Security, Subscription, Bug, Feature Request, Other).
  - **Sentiment Analysis**: Scikit-learn `TfidfVectorizer` paired with `LogisticRegression` for sentiment scoring (Positive, Neutral, Negative) combined with deterministic keyword heuristics for customer tone detection (Calm, Frustrated, Urgent, Angry, Satisfied, Confused).
  - **Duplicate Ticket Detection**: TF-IDF feature extraction with cosine similarity comparison across active customer candidate tickets.
- **Process Security**: Dedicated non-root user `appuser` (UID 1001), non-root group `appgroup` (GID 999).
- **Health Monitoring**: Native Python script inspecting `http://localhost:8000/health`.

### 5. PostgreSQL Database (`smartdesk-prod-postgres`)
- **Engine**: PostgreSQL 17 official image.
- **Storage Volume**: Named volume `smartdesk_postgres_data` mapped to `/var/lib/postgresql/data`.
- **Access Control**: Relational store bound strictly to `smartdesk-prod-network`. No external port published.
- **Schema Management**: 19 normalized relational tables with UUID primary keys, check constraints, foreign keys, triggers, and B-tree indexes.

### 6. Automated Backup Service (`smartdesk-prod-backup`)
- **Engine**: PostgreSQL 17 Alpine client.
- **Backup Mechanism**: `pg_dump` streaming through `gzip` compression into `/backups/smartdesk_backup_%Y%m%d_%H%M%S.sql.gz`.
- **Scheduling**: Crond running nightly at 02:00 UTC (`0 2 * * *`).
- **Retention**: Automated pruning of backup files older than 14 days.
- **Storage Volume**: Dedicated volume `smartdesk_prod_backups`.

---

## 4. Authentication, Authorization & Data Privacy

1. **Authentication**: Stateless HMAC-SHA384 JWT tokens issued on successful credentials verification (BCrypt work factor 12).
2. **Role-Based Access Control (RBAC)**:
   - `CUSTOMER`: Can create tickets, view own tickets, post customer replies, view own profile. Strictly isolated from other customer organizations.
   - `AGENT`: Can view queue, view assigned tickets, post replies, post internal staff notes, escalate tickets, update status, view AI metadata.
   - `ADMIN`: Full system superuser access including user administration, category management, team provisioning, SLA configurations, and system-wide analytics.
3. **AI Metadata Privacy Barrier**:
   - `sentimentAnalysis` and `duplicateMatches` fields in ticket details are populated **only** when requested by `AGENT` or `ADMIN`.
   - Customer API views return `null` for internal AI confidence scores, model versions, and duplicate match candidates.

---

## 5. High-Availability & Fault Tolerance Design

### AI Service Unavailable Fallback
The AI microservice is strictly non-critical for core ticketing operations:
1. When the backend creates a ticket, it attempts an HTTP POST to `http://ai-service:8000/api/v1/classify` with a 3000ms timeout.
2. If `ai-service` times out, is paused, or returns an error:
   - The backend logs a warning: `AI classification microservice unavailable or timed out. Continuing with fallback routing.`
   - `aiCategory`, `aiConfidence`, and `aiModelVersion` are set to `null`.
   - The ticket is safely persisted in PostgreSQL with status `OPEN`.
   - Fallback routing logic assigns the ticket to the default unassigned team triage queue.
   - Sentiment analysis and duplicate detection are bypassed gracefully without throwing client-facing errors.
   - The customer receives an `HTTP 201 Created` response with zero stack traces or internal errors.
