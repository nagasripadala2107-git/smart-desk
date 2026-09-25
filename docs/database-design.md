# SmartDesk Database Design & Relational Algebra (DBMS / DMGT)

This document provides a formal theoretical and practical database specification for **SmartDesk**. It pairs formal mathematical **Relational Algebra (Discrete Mathematical Structures / Graph Theory - DMGT)** with production ANSI/PostgreSQL SQL queries, followed by normalization analysis, integrity constraints, and architectural storage decisions.

---

## 1. Formal Relational Algebra & SQL Equivalence

Relational Algebra is a procedural query language consisting of operators that take one or two relations as input and produce a new relation as output. Below are the fundamental and composite operations applied to the SmartDesk relational schema.

---

### Example 1: Selection ($\sigma$)
**Mathematical Definition**:
The Selection operator filters tuples (rows) that satisfy a specified predicate condition $P$.
$$\sigma_P(R) = \{ t \in R \mid P(t) \}$$

**DMGT / Relational Algebra Expression**:
Filter all tickets that are currently in the `OPEN` state:
$$\sigma_{status = 'OPEN'}(Tickets)$$

**Equivalent PostgreSQL Query**:
```sql
SELECT *
FROM tickets
WHERE status = 'OPEN';
```

---

### Example 2: Projection ($\pi$)
**Mathematical Definition**:
The Projection operator selects specified attributes (columns) from a relation, discarding all other attributes and eliminating duplicate tuples.
$$\pi_{A_1, A_2, \dots, A_n}(R)$$

**DMGT / Relational Algebra Expression**:
Extract only the ticket identification number, subject, and priority level:
$$\pi_{ticket\_number, subject, priority}(Tickets)$$

**Equivalent PostgreSQL Query**:
```sql
SELECT DISTINCT ticket_number, subject, priority
FROM tickets;
```

---

### Example 3: Selection + Projection ($\pi(\sigma)$)
**Mathematical Definition**:
Composition of unary operators. First, filter tuples based on predicate $P$, then project the designated subset of attributes $A$.
$$\pi_{A_1, \dots, A_n}(\sigma_P(R))$$

**DMGT / Relational Algebra Expression**:
Retrieve the ticket number, subject, and created timestamp for all tickets having `URGENT` priority:
$$\pi_{ticket\_number, subject, created\_at}(\sigma_{priority = 'URGENT'}(Tickets))$$

**Equivalent PostgreSQL Query**:
```sql
SELECT ticket_number, subject, created_at
FROM tickets
WHERE priority = 'URGENT';
```

---

### Example 4: Natural / Equi-Join ($\bowtie$)
**Mathematical Definition**:
The Theta Join / Equi-Join combines related tuples from two relations based on an equality condition across common or designated attributes.
$$R \bowtie_{R.A = S.B} S = \sigma_{R.A = S.B}(R \times S)$$

**DMGT / Relational Algebra Expression**:
Join the `Customers` relation with the `Tickets` relation on matching customer identifiers:
$$Customers \bowtie_{Customers.id = Tickets.customer\_id} Tickets$$

**Equivalent PostgreSQL Query**:
```sql
SELECT
    c.customer_code,
    c.company_name,
    c.plan,
    t.ticket_number,
    t.subject,
    t.status,
    t.priority
FROM customers c
INNER JOIN tickets t ON c.id = t.customer_id;
```

---

### Example 5: Selection + Join ($\sigma(R \bowtie S)$)
**Mathematical Definition**:
Apply a relational join across two entities, followed by a selection filter on an attribute from either relation.
$$\sigma_{P}(R \bowtie_{R.A = S.B} S)$$

**DMGT / Relational Algebra Expression**:
Find all tickets submitted by Enterprise customers that are currently `OPEN`:
$$\sigma_{c.plan = 'ENTERPRISE' \land t.status = 'OPEN'}(Customers \bowtie_{c.id = t.customer\_id} Tickets)$$

**Equivalent PostgreSQL Query**:
```sql
SELECT
    c.company_name,
    c.customer_code,
    t.ticket_number,
    t.subject,
    t.priority,
    t.created_at
FROM customers c
INNER JOIN tickets t ON c.id = t.customer_id
WHERE c.plan = 'ENTERPRISE'
  AND t.status = 'OPEN';
```

---

### Example 6: Aggregation ($\gamma$)
**Mathematical Definition**:
The Aggregate operator applies summary functions (COUNT, SUM, AVG, MIN, MAX) over groupings of attributes.
$$_{G_1, \dots, G_k}\gamma_{F_1(A_1), \dots, F_m(A_m)}(R)$$

**DMGT / Relational Algebra Expression**:
Count the number of support tickets grouped by each category:
$$_{category\_id}\gamma_{COUNT(id) \to total\_tickets}(Tickets)$$

**Equivalent PostgreSQL Query**:
```sql
SELECT
    c.name AS category_name,
    COUNT(t.id) AS total_tickets
FROM categories c
LEFT JOIN tickets t ON c.id = t.category_id
GROUP BY c.id, c.name
ORDER BY total_tickets DESC;
```

---

### Example 7: Selection, Multi-Table Join, and Grouped Aggregation
**Mathematical Definition**:
Complex relational composition involving three relations ($Agent \bowtie Ticket \bowtie Team$), filtering on operational criteria, and grouping by agent to calculate current active workload.

**DMGT / Relational Algebra Expression**:
Calculate active ticket count for each available agent belonging to the Technical Support team:
$$_{a.employee\_code, p.first\_name, p.last\_name}\gamma_{COUNT(t.id) \to active\_load}($$
$$\sigma_{tm.name = 'Technical Support' \land a.availability\_status = 'AVAILABLE' \land t.status \in \{'OPEN', 'IN\_PROGRESS'\}}($$
$$(Agents \bowtie Users \bowtie Profiles) \bowtie Teams \bowtie Tickets))$$

**Equivalent PostgreSQL Query**:
```sql
SELECT
    a.employee_code,
    p.first_name || ' ' || p.last_name AS agent_name,
    tm.name AS team_name,
    a.max_active_tickets,
    COUNT(t.id) AS current_active_tickets
FROM agents a
INNER JOIN users u ON a.user_id = u.id
INNER JOIN profiles p ON u.id = p.user_id
INNER JOIN teams tm ON a.team_id = tm.id
LEFT JOIN tickets t ON a.id = t.assigned_agent_id
                   AND t.status IN ('OPEN', 'IN_PROGRESS')
WHERE tm.name = 'Technical Support'
  AND a.availability_status = 'AVAILABLE'
GROUP BY a.id, a.employee_code, p.first_name, p.last_name, tm.name, a.max_active_tickets
HAVING COUNT(t.id) < a.max_active_tickets;
```

---

### Example 8: Set Difference ($-$)
**Mathematical Definition**:
Returns tuples that exist in relation $R$ but do not exist in relation $S$.
$$R - S = \{ t \in R \mid t \notin S \}$$

**DMGT / Relational Algebra Expression**:
Identify all customers who have never opened a ticket:
$$\pi_{id}(Customers) - \pi_{customer\_id}(Tickets)$$

**Equivalent PostgreSQL Query**:
```sql
SELECT c.id, c.customer_code, c.company_name
FROM customers c
EXCEPT
SELECT c.id, c.customer_code, c.company_name
FROM customers c
INNER JOIN tickets t ON c.id = t.customer_id;
```
*(Or via correlated `NOT EXISTS`)*:
```sql
SELECT c.id, c.customer_code, c.company_name
FROM customers c
WHERE NOT EXISTS (
    SELECT 1 FROM tickets t WHERE t.customer_id = c.id
);
```

---

## 2. Normalization Analysis

The SmartDesk database design strictly conforms to **Boyce-Codd Normal Form (BCNF)**.

### First Normal Form (1NF)
- **Requirement**: Each table column must contain only atomic (indivisible) values, and each record must be unique.
- **Implementation**:
  - All attributes represent singular atomic scalar values (`VARCHAR`, `UUID`, `TIMESTAMPTZ`, `INT`, `BOOLEAN`).
  - No repeating groups or multi-valued attributes exist in relational entities.
  - Multi-valued collections (e.g., ticket messages, attachments, assignment logs) are normalized into independent child relations (`ticket_messages`, `ticket_attachments`, `ticket_assignments`).

### Second Normal Form (2NF)
- **Requirement**: Must be in 1NF and have no partial dependencies (every non-key attribute must depend on the *entire* primary key).
- **Implementation**:
  - All tables utilize a single synthetic surrogate key (`id UUID` or `id BIGSERIAL`).
  - Because no composite primary keys are used, partial key dependencies are mathematically impossible ($100\%$ immune to partial dependency).

### Third Normal Form (3NF)
- **Requirement**: Must be in 2NF and have no transitive dependencies (non-key attributes must not depend on other non-key attributes: $X \to Y \to Z$).
- **Implementation**:
  - `Customer` attributes (company name, account tier) are isolated in `customers`, not duplicated in `tickets`. `tickets` only stores the foreign key reference `customer_id`.
  - `Agent` team assignments are isolated via `team_id` rather than duplicating team name or description inside `agents` or `tickets`.
  - `Profiles` separates personal details (first name, phone) from the authentication record in `users`.

### Boyce-Codd Normal Form (BCNF)
- **Requirement**: For every functional dependency $X \to Y$, $X$ must be a superkey.
- **Implementation**:
  - In `users`, `email \to (password_hash, role, is_active)`; `email` is designated `UNIQUE` (a candidate superkey).
  - In `customers`, `customer_code \to (company_name, plan)`; `customer_code` is `UNIQUE`.
  - In `agents`, `employee_code \to (team_id, availability_status)`; `employee_code` is `UNIQUE`.
  - In `tickets`, `ticket_number \to (subject, status, priority, ...)`; `ticket_number` is `UNIQUE`.
  - All determinant attribute sets are superkeys.

---

## 3. Database Integrity Constraints

| Constraint Type | Implementation | Example in SmartDesk |
|---|---|---|
| **Entity Integrity** | Primary Keys (`PRIMARY KEY`) | `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` guarantees distinct row identity. |
| **Referential Integrity** | Foreign Keys (`REFERENCES`) | `tickets.customer_id REFERENCES customers(id)` ensures orphaned tickets cannot exist. |
| **Domain Integrity** | `NOT NULL`, Data Types, `CHECK` constraints | `priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')` enforces strictly permitted values. |
| **User-Defined Integrity** | Triggers and Multi-Column Checks | `resolution_minutes >= first_response_minutes` in `sla_policies` guarantees logical time progression. |

---

## 4. Key Design Decisions

### A. UUID vs BIGSERIAL
- **UUID (`gen_random_uuid()`)** is used for core domain entities (`users`, `tickets`, `customers`, etc.).
  - *Benefits*: Prevents ID enumeration attacks in public URLs/APIs, facilitates distributed generation, eliminates database ID collision during distributed replication.
- **BIGSERIAL (64-bit integer)** is intentionally used for `audit_logs`.
  - *Benefits*: Audit records are append-only, high-frequency internal events. 64-bit integers minimize B-tree index depth and optimize sequential append performance on disk.

### B. Timestamps with Time Zone (`TIMESTAMPTZ`)
- PostgreSQL `TIMESTAMPTZ` internally stores epoch timestamps normalized to UTC.
- Prevents timezone skew and daylight saving conversion errors between global customer support teams and backend microservices.

### C. Decimal Representation for AI Confidence
- `NUMERIC(5, 4)` is used for `ai_confidence` with a `CHECK (ai_confidence BETWEEN 0.0 AND 1.0)`.
- Prevents binary floating-point inaccuracies (`float4`/`float8`) when comparing confidence thresholds (e.g. evaluating `>= 0.8500`).

### D. Semi-Structured Data (`JSONB`)
- Used for `ticket_events.metadata`, `audit_logs.old_data`, and `audit_logs.new_data`.
- Allows flexible snapshot capture of diverse payload shapes without sacrificing the queryability and indexing benefits of PostgreSQL binary JSON.
