# SmartDesk Database Schemas & Migrations

Welcome to the **SmartDesk** PostgreSQL Database Foundation. This directory houses the production-ready Data Definition Language (DDL) schemas, performance indexing strategies, reference/seed data, and versioned migration scripts for the SmartDesk intelligent ticket routing platform.

---

## 1. Database Purpose & Architecture

SmartDesk is an enterprise-grade AI-powered customer support platform. The PostgreSQL database serves as the single source of truth for:
- Identity and Role-Based Access Control (Admin, Agent, Customer)
- Ticket lifecycle management, SLAs, and assignment tracking
- Multi-tier escalation pathways and team hierarchies
- Historical timelines, customer notes, and event audit streams
- AI classification metadata (inferred category, confidence scores, model versions)

The database schema is designed with high normalization (3NF/BCNF), strict relational integrity constraints, sensible deletion cascades, and targeted composite indexes. It is optimized for future persistence via **Java Spring Boot (Spring Data JPA / Hibernate)**.

---

## 2. PostgreSQL Engine Requirements

- **Recommended Version**: PostgreSQL 14, 15, or 16+
- **Extensions**: `pgcrypto` (for native `gen_random_uuid()` fallback on older PostgreSQL distributions)
- **Timezone**: All timestamps use `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`) in UTC.

---

## 3. Directory Layout & File Descriptions

```
database/
├── README.md                      # This comprehensive setup and architectural guide
├── schema.sql                     # Complete DDL creating all 17 core tables, constraints, triggers
├── indexes.sql                    # Single-column and composite B-tree performance indexes
├── seed.sql                       # Reference baseline (Teams, Categories, SLA, Routing) + Demo Users
└── migrations/
    └── 001_initial_schema.sql     # Versioned idempotent migration combining DDL, indexes, and reference seeds
```

### File Details:
1. **`schema.sql`**:
   - Contains table definitions for all 17 core entities with primary keys, unique constraints, check constraints, foreign keys, and `updated_at` automated trigger functions.
2. **`indexes.sql`**:
   - Contains foreign key lookup indexes (to eliminate full table scans during joins and cascade actions), status/priority queue indexes, customer timeline indexes, and composite indexes.
3. **`seed.sql`**:
   - Contains reference datasets: 6 Teams, 9 Categories, 4 SLA Policies, and default Routing Rules.
   - Contains development-only demo users (Customer, Agent, Admin) with BCrypt-hashed passwords (`Password123!`). No plaintext credentials are ever stored.
4. **`migrations/001_initial_schema.sql`**:
   - Consolidated baseline migration script wrapped in a transaction (`BEGIN ... COMMIT`). Ready for future execution via migration tools (Flyway or Liquibase) or manual application.

---

## 4. Entity Catalog (17 Tables)

| # | Table Name | Purpose | Primary Key | Key Foreign Keys |
|---|---|---|---|---|
| 1 | `users` | Core credentials, role, active status | UUID | - |
| 2 | `profiles` | Personal contact details, avatar | UUID | `user_id` -> `users(id)` (1:1) |
| 3 | `customers` | Organization, customer code, subscription plan | UUID | `user_id` -> `users(id)` (1:1) |
| 4 | `teams` | Support departments and triage queues | UUID | - |
| 5 | `agents` | Agent operational settings, skills, limits | UUID | `user_id` -> `users(id)`, `team_id` -> `teams(id)` |
| 6 | `categories` | Standardized ticket classification taxonomy | UUID | - |
| 7 | `tickets` | Main operational ticket unit & AI scores | UUID | `customer_id`, `category_id`, `assigned_agent_id`, `assigned_team_id` |
| 8 | `ticket_messages`| Conversational thread and internal staff notes | UUID | `ticket_id` -> `tickets(id)`, `sender_user_id` -> `users(id)` |
| 9 | `ticket_attachments` | File metadata & object storage references | UUID | `ticket_id` -> `tickets(id)`, `message_id` -> `ticket_messages(id)` |
| 10 | `ticket_assignments`| Historical audit of agent/team assignment | UUID | `ticket_id`, `agent_id`, `team_id`, `assigned_by_user_id` |
| 11 | `ticket_events` | Immutable chronological event log | UUID | `ticket_id`, `actor_user_id` |
| 12 | `escalations` | Multi-tier escalation workflows & graph routing | UUID | `ticket_id`, `from_team_id`, `to_team_id`, `from_agent_id`, `to_agent_id` |
| 13 | `routing_rules` | Automated assignment rules based on category | UUID | `category_id` -> `categories(id)`, `team_id` -> `teams(id)` |
| 14 | `escalation_rules` | Automated threshold triggers (Time, Priority) | UUID | `from_team_id` -> `teams(id)`, `to_team_id` -> `teams(id)` |
| 15 | `sla_policies` | First-response and resolution SLA targets | UUID | - |
| 16 | `notifications` | In-app alerts and notifications | UUID | `user_id` -> `users(id)`, `ticket_id` -> `tickets(id)` |
| 17 | `audit_logs` | Administrative and system security logs | BIGSERIAL | `actor_user_id` -> `users(id)` |

---

## 5. Setup Guide for Windows (Step-by-Step)

When PostgreSQL is installed on your Windows machine, use the following beginner-friendly instructions.

### Option A: Using Local PostgreSQL (`psql`)

#### Step 1: Open PowerShell as Administrator
Open Windows PowerShell and verify that `psql` is recognized:
```powershell
psql --version
```

#### Step 2: Create the Database
Log into PostgreSQL via the default `postgres` superuser:
```powershell
psql -U postgres -c "CREATE DATABASE smartdesk;"
```
*(Enter your PostgreSQL postgres superuser password when prompted).*

#### Step 3: Run the SQL Scripts in Sequence
Navigate to the repository `database/` directory:
```powershell
cd C:\Users\prasanna\ticket\database
```

Execute the scripts in proper dependency order:
```powershell
# 1. Apply Schema DDL (Tables, Constraints, Triggers)
psql -U postgres -d smartdesk -f schema.sql

# 2. Apply Performance Indexes
psql -U postgres -d smartdesk -f indexes.sql

# 3. Apply Baseline Reference Data & Demo Seeds
psql -U postgres -d smartdesk -f seed.sql
```

*Alternatively, execute the single migration script instead:*
```powershell
psql -U postgres -d smartdesk -f migrations/001_initial_schema.sql
```

---

### Option B: Using Docker (Recommended for Isolated Development)

If you have Docker Desktop installed, you can start a clean PostgreSQL container with a single command:

```powershell
# Start PostgreSQL container
docker run --name smartdesk-postgres -e POSTGRES_DB=smartdesk -e POSTGRES_USER=smartdesk -e POSTGRES_PASSWORD=smartdesk_secret -p 5432:5432 -d postgres:16-alpine

# Execute migration script into the container
docker exec -i smartdesk-postgres psql -U smartdesk -d smartdesk < C:\Users\prasanna\ticket\database\migrations\001_initial_schema.sql

# Or execute seed script
docker exec -i smartdesk-postgres psql -U smartdesk -d smartdesk < C:\Users\prasanna\ticket\database\seed.sql
```

---

### Option C: Using pgAdmin GUI
1. Open **pgAdmin**.
2. Connect to your local PostgreSQL server.
3. Right-click on **Databases** > **Create** > **Database...** -> Enter name `smartdesk` and click **Save**.
4. Right-click the newly created `smartdesk` database and select **Query Tool**.
5. Click the folder icon ("Open File") and select `schema.sql`, then click the **Execute / Run** button (F5).
6. Repeat for `indexes.sql` and `seed.sql`.

---

## 6. Seed Data & Demo Accounts

The `seed.sql` script creates baseline operational datasets and development test accounts.

### Demo User Accounts:
> [!NOTE]
> All demo accounts have their password set to: **`Password123!`**
> In compliance with security standards, all passwords in `seed.sql` are stored as BCrypt hashes (`$2a$12$...`). Plaintext passwords are NEVER stored in database scripts.

| Role | Email | Name / Company | Initial Assignment / Code |
|---|---|---|---|
| **ADMIN** | `admin@smartdesk.local` | System Administrator | Full access |
| **AGENT** | `agent.billing@smartdesk.local` | Alice Billington | Billing Team (`EMP-BIL-101`) |
| **AGENT** | `agent.tech@smartdesk.local` | Bob Technician | Technical Support (`EMP-TEC-102`) |
| **AGENT** | `agent.security@smartdesk.local` | Sam Securitas | Security Team (`EMP-SEC-103`) |
| **CUSTOMER** | `alex@acmecorp.local` | Alex Acme (Acme Corp) | Enterprise Plan (`CUST-ACME-001`) |
| **CUSTOMER** | `sara@globex.local` | Sara Globex (Globex Int) | Premium Plan (`CUST-GLBX-002`) |

---

## 7. Future Spring Boot / JPA Integration Notes

When developing Phase 3 (Java Spring Boot backend):

1. **Entity Identifier Mapping**:
   Use Java `java.util.UUID` with `@GeneratedValue(strategy = GenerationType.AUTO)` or Hibernate's `@UuidGenerator`:
   ```java
   @Id
   @GeneratedValue
   @Column(columnDefinition = "UUID")
   private UUID id;
   ```
2. **Audit Logs Primary Key**:
   Mapped as `Long` / `BigInteger` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
3. **JSONB Columns**:
   Use Hypersistence Utilities (`@Type(JsonType.class)`) or Spring Data JPA 3.x attribute converters for `ticket_events.metadata`, `audit_logs.old_data`, and `audit_logs.new_data`.
4. **Enums**:
   Map `role`, `priority`, `status`, and `availability_status` using `@Enumerated(EnumType.STRING)` to strictly bind to database `VARCHAR` check constraints.
5. **Timestamps**:
   Map `TIMESTAMPTZ` to Java 8 `java.time.OffsetDateTime` or `java.time.Instant`.
