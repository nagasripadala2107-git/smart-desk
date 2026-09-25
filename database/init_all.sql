-- =============================================================================
-- SMARTDESK DATABASE MIGRATION: V001_initial_schema.sql
-- Description: Baseline schema, core tables, triggers, indexes, and reference seeds
-- Dialect: PostgreSQL 14+
-- Target: Flyway / Liquibase / Manual execution
-- Note: Idempotent and transactionally wrapped
-- =============================================================================

BEGIN;

-- =============================================================================
-- 0. EXTENSIONS & FUNCTIONS
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION set_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 1. CORE TABLES
-- =============================================================================

-- 1.1 Users
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

-- 1.2 Profiles
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

-- 1.3 Customers
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

-- 1.4 Teams
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

-- 1.5 Agents
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

-- 1.6 Categories
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

-- 1.7 Tickets
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

-- 1.8 Ticket Messages
CREATE TABLE IF NOT EXISTS ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    message TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_ticket_messages_updated_at
BEFORE UPDATE ON ticket_messages
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_column();

-- 1.9 Ticket Attachments
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

-- 1.10 Ticket Assignments (History)
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

-- 1.11 Ticket Events (Audit Timeline)
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

-- 1.12 Escalations
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

-- 1.13 Routing Rules
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

-- 1.14 Escalation Rules
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

-- 1.15 SLA Policies
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

-- 1.16 Notifications
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

-- 1.17 Audit Logs
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
-- 2. INDEXES
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_agents_user_id ON agents(user_id);
CREATE INDEX IF NOT EXISTS idx_agents_team_id ON agents(team_id);
CREATE INDEX IF NOT EXISTS idx_agents_availability ON agents(availability_status);

CREATE INDEX IF NOT EXISTS idx_tickets_ticket_number ON tickets(ticket_number);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_category_id ON tickets(category_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_agent_id ON tickets(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_team_id ON tickets(assigned_team_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority ON tickets(priority);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at DESC);

-- Composite Indexes
CREATE INDEX IF NOT EXISTS idx_tickets_status_priority ON tickets(status, priority);
CREATE INDEX IF NOT EXISTS idx_tickets_team_status ON tickets(assigned_team_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_agent_status ON tickets(assigned_agent_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_created ON tickets(customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_created_at ON ticket_messages(ticket_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_internal ON ticket_messages(ticket_id, is_internal);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_message_id ON ticket_attachments(message_id);

CREATE INDEX IF NOT EXISTS idx_ticket_assignments_ticket_id ON ticket_assignments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_agent_id ON ticket_assignments(agent_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_team_id ON ticket_assignments(team_id);

CREATE INDEX IF NOT EXISTS idx_ticket_events_ticket_id ON ticket_events(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_events_ticket_created ON ticket_events(ticket_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_ticket_events_event_type ON ticket_events(event_type);

CREATE INDEX IF NOT EXISTS idx_escalations_ticket_id ON escalations(ticket_id);
CREATE INDEX IF NOT EXISTS idx_escalations_status ON escalations(status);
CREATE INDEX IF NOT EXISTS idx_escalations_to_team ON escalations(to_team_id, status);
CREATE INDEX IF NOT EXISTS idx_escalations_level ON escalations(level);

CREATE INDEX IF NOT EXISTS idx_routing_rules_lookup ON routing_rules(category_id, priority, is_active);
CREATE INDEX IF NOT EXISTS idx_escalation_rules_trigger ON escalation_rules(from_team_id, trigger_type, is_active);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_ticket_id ON notifications(ticket_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =============================================================================
-- 3. BASELINE REFERENCE SEEDS
-- =============================================================================

-- Teams
INSERT INTO teams (id, name, description, is_active) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'General Support', 'First-line triage and general inquiries team', TRUE),
    ('b0000000-0000-0000-0000-000000000002', 'Billing', 'Handles subscriptions, invoicing, and payment inquiries', TRUE),
    ('b0000000-0000-0000-0000-000000000003', 'Technical Support', 'Handles technical defects, server diagnostics, and code issues', TRUE),
    ('b0000000-0000-0000-0000-000000000004', 'Security', 'Handles security vulnerabilities, incident response, and compliance', TRUE),
    ('b0000000-0000-0000-0000-000000000005', 'Senior Support', 'Tier 2 escalation team for complex technical and operational issues', TRUE),
    ('b0000000-0000-0000-0000-000000000006', 'Management', 'Executive oversight and high-priority escalation resolution', TRUE)
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

-- Categories
INSERT INTO categories (id, name, description, is_active) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'BILLING', 'Billing, invoices, and charge inquiries', TRUE),
    ('c0000000-0000-0000-0000-000000000002', 'TECHNICAL', 'Technical infrastructure and platform usage issues', TRUE),
    ('c0000000-0000-0000-0000-000000000003', 'ACCOUNT', 'Account access, credential reset, and permissions', TRUE),
    ('c0000000-0000-0000-0000-000000000004', 'REFUND', 'Refund requests and payment disputes', TRUE),
    ('c0000000-0000-0000-0000-000000000005', 'SECURITY', 'Security reports, potential compromises, and audit questions', TRUE),
    ('c0000000-0000-0000-0000-000000000006', 'SUBSCRIPTION', 'Plan changes, upgrades, and renewals', TRUE),
    ('c0000000-0000-0000-0000-000000000007', 'BUG', 'Software anomalies, errors, and unexpected behavior', TRUE),
    ('c0000000-0000-0000-0000-000000000008', 'FEATURE_REQUEST', 'Product enhancements and feature suggestions', TRUE),
    ('c0000000-0000-0000-0000-000000000009', 'OTHER', 'Unclassified questions and general inquiries', TRUE)
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

-- SLA Policies
INSERT INTO sla_policies (id, name, priority, first_response_minutes, resolution_minutes, is_active) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'Standard Low Priority SLA', 'LOW', 1440, 2880, TRUE),
    ('d0000000-0000-0000-0000-000000000002', 'Standard Medium Priority SLA', 'MEDIUM', 480, 1440, TRUE),
    ('d0000000-0000-0000-0000-000000000003', 'Standard High Priority SLA', 'HIGH', 120, 480, TRUE),
    ('d0000000-0000-0000-0000-000000000004', 'Critical Urgent Priority SLA', 'URGENT', 30, 120, TRUE)
ON CONFLICT (priority) DO UPDATE
SET first_response_minutes = EXCLUDED.first_response_minutes,
    resolution_minutes = EXCLUDED.resolution_minutes;

-- Routing Rules
INSERT INTO routing_rules (id, name, category_id, priority, team_id, priority_weight, is_active) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'Billing Route', 'c0000000-0000-0000-0000-000000000001', NULL, 'b0000000-0000-0000-0000-000000000002', 10, TRUE),
    ('e0000000-0000-0000-0000-000000000002', 'Refund Route', 'c0000000-0000-0000-0000-000000000004', NULL, 'b0000000-0000-0000-0000-000000000002', 15, TRUE),
    ('e0000000-0000-0000-0000-000000000003', 'Subscription Route', 'c0000000-0000-0000-0000-000000000006', NULL, 'b0000000-0000-0000-0000-000000000002', 10, TRUE),
    ('e0000000-0000-0000-0000-000000000004', 'Technical Route', 'c0000000-0000-0000-0000-000000000002', NULL, 'b0000000-0000-0000-0000-000000000003', 10, TRUE),
    ('e0000000-0000-0000-0000-000000000005', 'Bug Route', 'c0000000-0000-0000-0000-000000000007', NULL, 'b0000000-0000-0000-0000-000000000003', 15, TRUE),
    ('e0000000-0000-0000-0000-000000000006', 'Security Route', 'c0000000-0000-0000-0000-000000000005', NULL, 'b0000000-0000-0000-0000-000000000004', 30, TRUE),
    ('e0000000-0000-0000-0000-000000000007', 'Account Route', 'c0000000-0000-0000-0000-000000000003', NULL, 'b0000000-0000-0000-0000-000000000001', 5, TRUE),
    ('e0000000-0000-0000-0000-000000000008', 'Feature Request Route', 'c0000000-0000-0000-0000-000000000008', NULL, 'b0000000-0000-0000-0000-000000000001', 5, TRUE),
    ('e0000000-0000-0000-0000-000000000009', 'General Fallback Route', 'c0000000-0000-0000-0000-000000000009', NULL, 'b0000000-0000-0000-0000-000000000001', 1, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Escalation Rules
INSERT INTO escalation_rules (id, name, from_team_id, to_team_id, trigger_type, trigger_value, escalation_level, is_active) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'General to Senior Escalation (Time)', 'b0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', 'TIME', '1440', 1, TRUE),
    ('f0000000-0000-0000-0000-000000000002', 'Technical to Senior Escalation (Priority)', 'b0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000005', 'PRIORITY', 'URGENT', 1, TRUE),
    ('f0000000-0000-0000-0000-000000000003', 'Security to Management Escalation (Urgent)', 'b0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000006', 'PRIORITY', 'URGENT', 2, TRUE)
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- SENTIMENT ANALYSIS EXTENSION
-- =============================================================================
-- =============================================================================
-- Migration: 002_sentiment_analysis.sql
-- Description: Phase 8.2 AI Sentiment Analysis & Customer Tone Detection
-- =============================================================================

-- 1. Ensure ticket_messages has a unique constraint on (id, ticket_id)
-- This enables composite foreign key verification to guarantee relationship integrity.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_ticket_messages_id_ticket_id'
    ) THEN
        ALTER TABLE ticket_messages ADD CONSTRAINT uq_ticket_messages_id_ticket_id UNIQUE (id, ticket_id);
    END IF;
END $$;

-- 2. Create ticket_sentiment_analysis table
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

-- 3. Indexes for query optimization
CREATE INDEX IF NOT EXISTS idx_sentiment_ticket_id ON ticket_sentiment_analysis(ticket_id);
CREATE INDEX IF NOT EXISTS idx_sentiment_message_id ON ticket_sentiment_analysis(message_id);
CREATE INDEX IF NOT EXISTS idx_sentiment_created_at ON ticket_sentiment_analysis(ticket_id, created_at DESC);

-- =============================================================================
-- DUPLICATE DETECTION EXTENSION
-- =============================================================================
-- =============================================================================
-- Migration: 003_duplicate_detection.sql
-- Description: Phase 8.3 AI Duplicate Ticket Detection
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

COMMIT;
