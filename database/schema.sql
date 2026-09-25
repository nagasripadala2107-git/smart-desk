-- =============================================================================
-- SMARTDESK DATABASE FOUNDATION — DDL SCHEMA DEFINITION
-- Dialect: PostgreSQL 14+
-- Module: Phase 2 Database Foundation
-- =============================================================================

-- Enable pgcrypto / uuid-ossp if gen_random_uuid() is needed on older versions,
-- though PostgreSQL 13+ includes gen_random_uuid() natively.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- TRIGGER HELPER FUNCTION: AUTOMATIC updated_at REFRESH
-- =============================================================================
CREATE OR REPLACE FUNCTION set_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 1. USERS TABLE
-- Authentication, roles, and account state
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'AGENT', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 2. PROFILES TABLE
-- Personal profile details (1-to-1 with users)
-- =============================================================================
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_profiles_updated_at
BEFORE UPDATE ON profiles
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 3. CUSTOMERS TABLE
-- Customer account information (1-to-1 with users)
-- =============================================================================
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(255),
    plan VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_customers_updated_at
BEFORE UPDATE ON customers
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 4. TEAMS TABLE
-- Functional support teams / departments
-- =============================================================================
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_teams_updated_at
BEFORE UPDATE ON teams
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 5. AGENTS TABLE
-- Agent operational settings & team assignments (1-to-1 with users)
-- =============================================================================
CREATE TABLE IF NOT EXISTS agents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    availability_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE'
        CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'OFFLINE')),
    skills TEXT,
    max_active_tickets INT NOT NULL DEFAULT 5 CHECK (max_active_tickets >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_agents_updated_at
BEFORE UPDATE ON agents
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 6. CATEGORIES TABLE
-- Issue categories for classification and routing
-- =============================================================================
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
        CHECK (name IN ('BILLING', 'TECHNICAL', 'ACCOUNT', 'REFUND', 'SECURITY', 'SUBSCRIPTION', 'BUG', 'FEATURE_REQUEST', 'OTHER')),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 7. TICKETS TABLE
-- Core operational support ticket entity
-- =============================================================================
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    assigned_agent_id UUID REFERENCES agents(id) ON DELETE SET NULL,
    assigned_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'PENDING_CUSTOMER', 'PENDING_INTERNAL', 'ESCALATED', 'RESOLVED', 'CLOSED')),
    ai_category VARCHAR(50),
    ai_confidence NUMERIC(5, 4)
        CHECK (ai_confidence IS NULL OR (ai_confidence >= 0.0000 AND ai_confidence <= 1.0000)),
    ai_model_version VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ
);

CREATE TRIGGER trg_tickets_updated_at
BEFORE UPDATE ON tickets
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 8. TICKET_MESSAGES TABLE
-- Conversational messages and private internal notes
-- =============================================================================
CREATE TABLE IF NOT EXISTS ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    message TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ticket_messages_id_ticket_id UNIQUE (id, ticket_id)
);

CREATE TRIGGER trg_ticket_messages_updated_at
BEFORE UPDATE ON ticket_messages
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 9. TICKET_ATTACHMENTS TABLE
-- Metadata and cloud/object storage references for uploaded files
-- =============================================================================
CREATE TABLE IF NOT EXISTS ticket_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    message_id UUID REFERENCES ticket_messages(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 10. TICKET_ASSIGNMENTS TABLE
-- Historical log of ticket movement between agents and teams
-- =============================================================================
CREATE TABLE IF NOT EXISTS ticket_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    agent_id UUID REFERENCES agents(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    assigned_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assignment_reason TEXT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ
);

-- =============================================================================
-- 11. TICKET_EVENTS TABLE
-- Comprehensive audit trail and chronological timeline for tickets
-- =============================================================================
CREATE TABLE IF NOT EXISTS ticket_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'TICKET_CREATED',
        'CATEGORY_CHANGED',
        'PRIORITY_CHANGED',
        'STATUS_CHANGED',
        'ASSIGNED',
        'REASSIGNED',
        'ESCALATED',
        'MESSAGE_ADDED',
        'RESOLVED',
        'CLOSED'
    )),
    old_value TEXT,
    new_value TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 12. ESCALATIONS TABLE
-- Escalation workflows across teams and agents
-- =============================================================================
CREATE TABLE IF NOT EXISTS escalations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    from_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    to_team_id UUID NOT NULL REFERENCES teams(id) ON DELETE RESTRICT,
    from_agent_id UUID REFERENCES agents(id) ON DELETE SET NULL,
    to_agent_id UUID REFERENCES agents(id) ON DELETE SET NULL,
    level INT NOT NULL DEFAULT 1 CHECK (level >= 1),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

-- =============================================================================
-- 13. ROUTING_RULES TABLE
-- Automatic category & priority based ticket routing configuration
-- =============================================================================
CREATE TABLE IF NOT EXISTS routing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    priority VARCHAR(20) CHECK (priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    priority_weight INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_routing_rules_updated_at
BEFORE UPDATE ON routing_rules
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 14. ESCALATION_RULES TABLE
-- Trigger-based automatic escalation policy rules
-- =============================================================================
CREATE TABLE IF NOT EXISTS escalation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    from_team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    to_team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('TIME', 'PRIORITY', 'STATUS', 'MANUAL')),
    trigger_value VARCHAR(100) NOT NULL,
    escalation_level INT NOT NULL DEFAULT 1 CHECK (escalation_level >= 1),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_escalation_rules_updated_at
BEFORE UPDATE ON escalation_rules
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 15. SLA_POLICIES TABLE
-- Service Level Agreement targets by priority level
-- =============================================================================
CREATE TABLE IF NOT EXISTS sla_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    priority VARCHAR(20) NOT NULL UNIQUE CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    first_response_minutes INT NOT NULL CHECK (first_response_minutes > 0),
    resolution_minutes INT NOT NULL CHECK (resolution_minutes >= first_response_minutes),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_sla_policies_updated_at
BEFORE UPDATE ON sla_policies
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- =============================================================================
-- 16. NOTIFICATIONS TABLE
-- Real-time and persistent alerts for users regarding tickets
-- =============================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ticket_id UUID REFERENCES tickets(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ
);

-- =============================================================================
-- 17. AUDIT_LOGS TABLE
-- Append-only system-wide security, change tracking, and access logs
-- Sequential BIGSERIAL is justified for high-volume append-only audit records
-- =============================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 18. TICKET_SENTIMENT_ANALYSIS TABLE (Phase 8.2)
-- AI sentiment analysis and tone detection records for tickets and messages
-- =============================================================================
CREATE TABLE IF NOT EXISTS ticket_sentiment_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    message_id UUID REFERENCES ticket_messages(id) ON DELETE CASCADE,
    sentiment VARCHAR(20) NOT NULL CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    confidence NUMERIC(5, 4) NOT NULL CHECK (confidence >= 0.0000 AND confidence <= 1.0000),
    tone VARCHAR(30) NOT NULL CHECK (tone IN ('CALM', 'FRUSTRATED', 'URGENT', 'ANGRY', 'SATISFIED', 'CONFUSED', 'NEUTRAL')),
    model_version VARCHAR(50) NOT NULL,
    analyzed_text_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sentiment_ticket_message FOREIGN KEY (message_id, ticket_id) REFERENCES ticket_messages(id, ticket_id) ON DELETE CASCADE
);

-- =============================================================================
-- 19. TICKET_DUPLICATE_MATCHES TABLE (Phase 8.3)
-- Advisory duplicate matches computed by TF-IDF vectorization and cosine similarity
-- =============================================================================
CREATE TABLE IF NOT EXISTS ticket_duplicate_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    matched_ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    similarity_score NUMERIC(5, 4) NOT NULL CHECK (similarity_score >= 0.0000 AND similarity_score <= 1.0000),
    model_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ticket_duplicate_not_self CHECK (ticket_id <> matched_ticket_id),
    CONSTRAINT uq_ticket_duplicate_pair UNIQUE (ticket_id, matched_ticket_id)
);

CREATE INDEX IF NOT EXISTS idx_duplicate_ticket_id ON ticket_duplicate_matches(ticket_id);
CREATE INDEX IF NOT EXISTS idx_duplicate_matched_ticket_id ON ticket_duplicate_matches(matched_ticket_id);
CREATE INDEX IF NOT EXISTS idx_duplicate_created_at ON ticket_duplicate_matches(ticket_id, created_at DESC);
