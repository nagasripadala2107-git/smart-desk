# SmartDesk Database Schemas & Migrations

This directory stores relational database schema definitions, initial seed data, and migration scripts.

## Database
- **Engine**: PostgreSQL 16+
- **Core Entities**:
  - `User`, `Profile`, `Customer`, `Agent`, `Team`, `Category`
  - `Ticket`, `TicketMessage`, `TicketAssignment`, `TicketEvent`, `Escalation`
  - `RoutingRule`, `EscalationRule`, `SlaPolicy`, `Notification`, `AuditLog`
- **Integrations**: Spring Data JPA / Flyway / Liquibase migrations and SQL seed scripts.

*Note: Initialized as placeholder during Phase 1. Schemas, seed files, and relational algebra queries will be added in subsequent phases.*
