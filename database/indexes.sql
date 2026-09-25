-- =============================================================================
-- SMARTDESK DATABASE FOUNDATION — INDEX DEFINITIONS
-- Dialect: PostgreSQL 14+
-- Module: Phase 2 Database Foundation
-- =============================================================================

-- =============================================================================
-- 1. USERS & PROFILES INDEXES
-- Note: users(email), customers(customer_code), and agents(employee_code)
-- have UNIQUE constraints which PostgreSQL indexes automatically via B-tree.
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_agents_user_id ON agents(user_id);
CREATE INDEX IF NOT EXISTS idx_agents_team_id ON agents(team_id);
CREATE INDEX IF NOT EXISTS idx_agents_availability ON agents(availability_status);

-- =============================================================================
-- 2. TICKETS ACCESS PATTERN INDEXES
-- Frequent access paths: customer portals, agent queues, team queues, analytics
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_tickets_ticket_number ON tickets(ticket_number);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_category_id ON tickets(category_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_agent_id ON tickets(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_team_id ON tickets(assigned_team_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority ON tickets(priority);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at DESC);

-- Composite Indexes for High-Frequency Ticket Filters:
-- Accelerates "Active Open Tickets sorted by Priority"
CREATE INDEX IF NOT EXISTS idx_tickets_status_priority ON tickets(status, priority);

-- Accelerates Team Dashboard Views ("Tickets assigned to Team X by Status")
CREATE INDEX IF NOT EXISTS idx_tickets_team_status ON tickets(assigned_team_id, status);

-- Accelerates Agent "My Open Tickets" Workspaces
CREATE INDEX IF NOT EXISTS idx_tickets_agent_status ON tickets(assigned_agent_id, status);

-- Accelerates Customer Dashboard ("My Recent Tickets")
CREATE INDEX IF NOT EXISTS idx_tickets_customer_created ON tickets(customer_id, created_at DESC);

-- =============================================================================
-- 3. TICKET_MESSAGES & ATTACHMENTS INDEXES
-- Conversation thread retrieval, customer view separation (internal vs public)
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_created_at ON ticket_messages(ticket_id, created_at ASC);
-- Filter internal messages for customer portal visibility
CREATE INDEX IF NOT EXISTS idx_ticket_messages_internal ON ticket_messages(ticket_id, is_internal);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_message_id ON ticket_attachments(message_id);

-- =============================================================================
-- 4. TICKET ASSIGNMENTS & EVENTS (AUDIT & TIMELINE)
-- Chronological event feed and agent re-assignment tracking
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_ticket_id ON ticket_assignments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_agent_id ON ticket_assignments(agent_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_team_id ON ticket_assignments(team_id);

CREATE INDEX IF NOT EXISTS idx_ticket_events_ticket_id ON ticket_events(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_events_ticket_created ON ticket_events(ticket_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_ticket_events_event_type ON ticket_events(event_type);

-- =============================================================================
-- 5. ESCALATIONS INDEXES
-- Queue management for escalations and graph routing queries
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_escalations_ticket_id ON escalations(ticket_id);
CREATE INDEX IF NOT EXISTS idx_escalations_status ON escalations(status);
CREATE INDEX IF NOT EXISTS idx_escalations_to_team ON escalations(to_team_id, status);
CREATE INDEX IF NOT EXISTS idx_escalations_level ON escalations(level);

-- =============================================================================
-- 6. ROUTING & ESCALATION RULES
-- Fast evaluation of incoming ticket routing criteria
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_routing_rules_lookup ON routing_rules(category_id, priority, is_active);
CREATE INDEX IF NOT EXISTS idx_escalation_rules_trigger ON escalation_rules(from_team_id, trigger_type, is_active);

-- =============================================================================
-- 7. NOTIFICATIONS & AUDIT LOGS INDEXES
-- User notification inbox and administrative audit investigations
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_ticket_id ON notifications(ticket_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =============================================================================
-- 8. TICKET_SENTIMENT_ANALYSIS INDEXES (Phase 8.2)
-- Fast retrieval of latest sentiment for tickets and messages
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_sentiment_ticket_id ON ticket_sentiment_analysis(ticket_id);
CREATE INDEX IF NOT EXISTS idx_sentiment_message_id ON ticket_sentiment_analysis(message_id);
CREATE INDEX IF NOT EXISTS idx_sentiment_created_at ON ticket_sentiment_analysis(ticket_id, created_at DESC);
