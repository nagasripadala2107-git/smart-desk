# SmartDesk Backend (Java Spring Boot)

Welcome to the **SmartDesk Primary Business Backend**. Built with **Java 21+ (verified on Java 26)** and **Spring Boot 3+**, this service is the authoritative core of the SmartDesk intelligent customer support and ticket routing platform.

---

## 1. Purpose & Architecture

The Java Spring Boot backend serves as the source of truth for:
- Identity, authentication, and Role-Based Access Control (`CUSTOMER`, `AGENT`, `ADMIN`)
- Ticket ingestion, automated sequential numbering (`SD-YYYY-XXXXXX`), and lifecycle state machines
- Customer isolation and message privacy separation (internal staff notes vs public responses)
- Python AI microservice integration (`POST http://ai-service:8000/api/v1/classify`)
- Deterministic category-to-team routing based on configurable rules
- Multi-tier escalation case tracking
- System audit logging and real-time user notification dispatch

### High-Level Architecture
```text
[ Browser Clients: Customer / Agent / Admin ]
                     |
                     | HTTPS / JSON REST APIs (Bearer JWT)
                     v
+-------------------------------------------------------+
|             Java Spring Boot Application              |
|                     (Port 8080)                       |
|                                                       |
|  Controllers -> DTOs -> Services -> Repositories      |
|  - Spring Security (Stateless JWT, BCrypt)            |
|  - Spring Data JPA (Hibernate, PostgreSQL Dialect)    |
|  - Rule-Based Routing Engine                          |
|  - AI Classification Client (http://ai-service:8000)  |
+---------------------------+---------------------------+
                            |
             +--------------+--------------+
             |                             |
             v                             v
+-------------------------+   +-------------------------+
|   PostgreSQL Database   |   | Python FastAPI Service  |
|       (Port 5432)       |   |       (Port 8000)       |
|  - 17 Relational Tables |   | - TF-IDF Vectorizer     |
|  - ddl-auto = validate  |   | - Logistic Regression   |
+-------------------------+   +-------------------------+
```

---

## 2. Prerequisites & Environment

- **Java Development Kit (JDK)**: Java 21+ (verified on `26.0.2`)
- **Build Tool**: Apache Maven (bundled via **Maven Wrapper**, global Maven is NOT required)
- **Database Engine**: PostgreSQL 14+ (in Docker or local, configured via environment variables)
- **AI Microservice**: Running at `http://localhost:8000` (local) or `http://ai-service:8000` (Docker Compose)

---

## 3. Running the Backend

### A. Running in Docker Compose (Recommended)
The backend runs automatically as part of the full stack:
```bash
# From repository root
docker compose up -d backend
```
Health status is checked via native Java probe targeting `http://localhost:8080/actuator/health`.

### B. Running Locally
```powershell
# Check Maven Wrapper version and detected Java environment
.\mvnw.cmd -version

# Run complete test suite (uses embedded in-memory H2)
.\mvnw.cmd test

# Run Spring Boot local server
.\mvnw.cmd spring-boot:run
```
*(On Linux / macOS, use `./mvnw` instead of `.\mvnw.cmd`).*

---

## 4. Configuration & Environment Variables

The backend uses `application.yml` with external environment variable overrides. A template is provided in `.env.example`.

| Variable | Default Value | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port for REST APIs |
| `DB_HOST` | `localhost` | PostgreSQL host address |
| `DB_PORT` | `5432` | PostgreSQL database port |
| `DB_NAME` | `smartdesk` | Target PostgreSQL database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `JWT_SECRET` | *(256-bit default)* | HMAC-SHA256 signing secret key |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (24 hours in milliseconds) |
| `CORS_ALLOWED_ORIGINS`| `http://localhost:3000`| Allowed CORS frontend origins |

> [!IMPORTANT]
> **Database Safety Notice**:
> The backend sets `spring.jpa.hibernate.ddl-auto=validate`. Hibernate will **never** create, drop, or alter your production tables. The Phase 2 PostgreSQL DDL scripts (`database/schema.sql` and `database/migrations/001_initial_schema.sql`) remain the authoritative source of truth.

---

## 5. Directory Structure

```
backend-java/
├── pom.xml                                    # Dependencies and build plugins
├── mvnw                                       # Unix wrapper script
├── mvnw.cmd                                   # Windows wrapper script
├── .mvn/wrapper/maven-wrapper.properties      # Wrapper distribution config
├── .env.example                               # Environment variable template
├── README.md                                  # Documentation guide
└── src/
    ├── main/
    │   ├── java/com/smartdesk/
    │   │   ├── SmartDeskApplication.java      # Application entrypoint
    │   │   ├── config/                        # SecurityConfig, WebConfig (CORS)
    │   │   ├── security/                      # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
    │   │   ├── entity/                        # 17 JPA Entities matching Phase 2 schema
    │   │   │   └── enums/                     # UserRole, TicketPriority, TicketStatus, etc.
    │   │   ├── repository/                    # 17 Spring Data JPA Repositories
    │   │   ├── dto/                           # DTOs: auth, customer, agent, ticket, message, escalation
    │   │   ├── mapper/                        # EntityDtoMapper decoupling persistence from API
    │   │   ├── exception/                     # GlobalExceptionHandler with standard ErrorResponse
    │   │   ├── service/                       # Business logic services & TicketNumberGenerator
    │   │   ├── routing/                       # TicketRoutingService (rule-based engine)
    │   │   ├── escalation/                    # EscalationPathService (boundary for Phase 7 graph)
    │   │   └── controller/                    # REST Controllers (/api/v1/*)
    │   └── resources/
    │       ├── application.yml                # Main production/development configuration
    │       └── application-dev.yml            # Local development log profile
    └── test/
        ├── java/com/smartdesk/                # Test suite (Context, Auth, Ticket, Access, Routing)
        └── resources/
            └── application-test.yml           # In-memory H2 configuration for automated tests
```

---

## 6. REST API Overview

All API endpoints are versioned under `/api/v1`.

### Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` — Register a customer or agent account (hashes password with BCrypt).
- `POST /api/v1/auth/login` — Authenticate and receive a signed JWT Bearer token.
- `POST /api/v1/auth/logout` — Logout user session.
- `GET /api/v1/auth/me` — Return the authenticated user profile.

### Customer Operations (`/api/v1/customer`)
- `GET /api/v1/customer/profile` — Get Customer 360 overview (organization, total tickets, active tickets).
- `GET /api/v1/customer/tickets` — List tickets submitted by the authenticated customer.

### Agent Workspace (`/api/v1/agent`)
- `GET /api/v1/agent/profile` — Get agent details, availability status, active workload count.
- `GET /api/v1/agent/queue` — Get agent inbox (assigned tickets and unassigned team pool).
- `GET /api/v1/agent/tickets` — List tickets assigned to the authenticated agent.

### Tickets (`/api/v1/tickets`)
- `POST /api/v1/tickets` — Submit a new ticket (generates `SD-YYYY-XXXXXX`, evaluates routing rule).
- `GET /api/v1/tickets` — List tickets (filtered by customer ownership or staff permissions).
- `GET /api/v1/tickets/{id}` — Get ticket detail with discussion thread and audit timeline.
- `PATCH /api/v1/tickets/{id}` — Update status, priority, category, or assignment.
- `POST /api/v1/tickets/{id}/auto-route` — Placeholder service boundary for intelligent routing.

### Ticket Messages (`/api/v1/tickets/{id}/messages`)
- `GET /api/v1/tickets/{id}/messages` — Get ticket messages (customers only see public messages; agents see internal notes).
- `POST /api/v1/tickets/{id}/messages` — Post message (customers cannot post internal notes).

### Escalation (`/api/v1/tickets/{id}/escalate`)
- `POST /api/v1/tickets/{id}/escalate` — Escalate ticket to a senior team/agent (transitions ticket to `ESCALATED`).

### Analytics & System (`/api/v1/analytics`, `/actuator`)
- `GET /api/v1/analytics/overview` — Total, open, in-progress, resolved, escalated counts.
- `GET /api/v1/analytics/tickets-by-category` — Ticket distribution across categories.
- `GET /api/v1/analytics/tickets-by-priority` — Ticket distribution across priorities.
- `GET /actuator/health` — Application health check.

---

## 7. Security & Role-Based Access Control (RBAC)

- **Authentication**: Stateless JWT Bearer tokens passed via HTTP `Authorization: Bearer <token>` header.
- **Passwords**: Hashed with BCrypt (cost factor 12). Plaintext passwords are never logged or stored.
- **Customer Isolation**: Verified at the service layer; customers cannot access or query other organizations' tickets.
- **Internal Note Privacy**: Customers can never view messages flagged `is_internal = true`. Staff notes remain confidential.
- **Exception Shielding**: `GlobalExceptionHandler` intercepts exceptions and returns sanitized JSON error payloads, preventing stack trace disclosure.

---

## 8. Automated Testing & Verification

The test suite runs automatically via the Maven Wrapper:
```powershell
.\mvnw.cmd test
```

### Verified Test Suites:
1. `SmartDeskApplicationTests`: Application context loading and bean wiring.
2. `AuthServiceTest`: User registration, BCrypt password hashing, JWT token generation, invalid password rejection, duplicate email detection.
3. `TicketServiceTest`: Ticket creation, `SD-YYYY-XXXXXX` format validation, valid status progression, invalid status transition rejection.
4. `TicketAccessControlTest`: Customer isolation enforcement, internal staff note hiding from customers, agent access rights.
5. `TicketRoutingServiceTest`: Deterministic category and priority routing rule matching.
