# SmartDesk 🚀
### AI-Powered Customer Support & Intelligent Ticket Routing Platform

SmartDesk is an enterprise-grade customer support platform designed with a clean micro-service architecture. It combines a dynamic Next.js frontend, a robust Java Spring Boot primary backend, a dedicated Python FastAPI machine learning classification service, and a PostgreSQL relational data store.

---

## 🏛️ System Architecture

```text
               +-----------------------------+
               |       Next.js Frontend      |
               | (React / TypeScript / CSS)  |
               +--------------+--------------+
                              |
                              | REST APIs / JSON
                              v
               +-----------------------------+
               |  Java Spring Boot Backend   |
               |   (Business Core & Rules)   |
               +-------+-------------+-------+
                       |             |
     Classify Request  |             | SQL / Transactions
     & Confidence      v             v
+------------------------+      +-------------------------+
| Python FastAPI Service |      |   PostgreSQL Database   |
| (TF-IDF + LogReg ML)   |      |  (ACID Relational Core) |
+------------------------+      +-------------------------+
```

### Architecture Guarantees
- **Java Spring Boot** acts as the single primary business backend orchestrating data persistence, authorization, ticket routing, and escalation.
- **Python FastAPI** operates purely as an isolated, dedicated ML service for categorization.
- **Strict Separation**: Frontend communicates only with the Java backend; client browsers never interact directly with the database or AI inference engine.

---

## 📁 Repository Structure

```text
smartdesk/
├── frontend/           # Next.js customer, agent & admin web interface
├── backend-java/       # Java Spring Boot enterprise backend service (Maven Wrapper)
├── ai-service/         # Python FastAPI text classification & AI triage service
├── database/           # Relational schema migrations, DDL, and seed datasets
├── docs/               # Technical specs, architecture docs, DMGT/ADSA references
├── docker/             # Dockerfiles and environment configurations
├── .gitignore          # Monorepo-wide version control ignore rules
└── README.md           # Project overview and quick start guide
```

---

## 🛠️ Technology Stack

| Layer | Technologies | Purpose |
| :--- | :--- | :--- |
| **Frontend** | Next.js, React, TypeScript, Tailwind CSS, Lucide, Recharts | Interactive, accessible portal for customers, agents, and administrators |
| **Backend** | Java 21+, Spring Boot 3+, Spring Data JPA, Spring Security | Primary enterprise business logic, routing engine, graph escalation, and RBAC |
| **AI / ML** | Python 3.14+, FastAPI, scikit-learn (TF-IDF + Logistic Regression) | Automatic ticket classification, confidence scoring, and model versioning |
| **Database** | PostgreSQL | Relational transactional persistence with strict integrity constraints |
| **Build & Dev** | Maven Wrapper (`mvnw`), npm, Git, Docker Compose | Consistent, reproducible local development and build pipelines |

---

## 🗺️ Project Phases

- [x] **Phase 1: Foundation & Repository Structure** *(Current)*
  - Monorepo directory setup, `.gitignore`, documentation, and architecture baseline.
- [ ] **Phase 2: Database Schema & Entity Design**
  - PostgreSQL tables, relational constraints, entity modeling, and relational algebra documentation.
- [ ] **Phase 3: Python AI Microservice**
  - FastAPI setup, training dataset, TF-IDF + Logistic Regression model pipeline, and inference endpoint.
- [ ] **Phase 4: Java Spring Boot Core Backend**
  - Maven Wrapper initialization, entity mappings, JPA repositories, REST controllers, and AI integration client.
- [ ] **Phase 5: Intelligent Routing Engine & Escalation Graph**
  - Workload-balanced agent assignment, graph traversal algorithms (BFS/DFS) for team escalation.
- [ ] **Phase 6: Spring Security & RBAC**
  - Secure authentication, customer vs. agent vs. admin access control, audit logging.
- [ ] **Phase 7: Next.js Frontend Application**
  - Responsive dashboards for Customer, Agent, and Admin portals.
- [ ] **Phase 8: Dockerization, Testing & Deployment**
  - Multi-service Docker Compose orchestration and end-to-end verification.

---

## 📖 Documentation
Detailed technical specifications and design patterns are available in [`docs/`](file:///c:/Users/prasanna/ticket/docs):
- [Architecture Blueprint](file:///c:/Users/prasanna/ticket/docs/architecture.md)
