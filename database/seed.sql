-- =============================================================================
-- SMARTDESK DATABASE FOUNDATION — SEED & REFERENCE DATA
-- Dialect: PostgreSQL 14+
-- Module: Phase 2 Database Foundation
-- Note: All passwords use BCrypt ($2a$12$...) hashes. Plaintext is NEVER stored.
-- Default development demo password for all accounts is: Password123!
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. REFERENCE TEAMS
-- =============================================================================
INSERT INTO teams (id, name, description, is_active) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'General Support', 'First-line triage and general inquiries team', TRUE),
    ('b0000000-0000-0000-0000-000000000002', 'Billing', 'Handles subscriptions, invoicing, and payment inquiries', TRUE),
    ('b0000000-0000-0000-0000-000000000003', 'Technical Support', 'Handles technical defects, server diagnostics, and code issues', TRUE),
    ('b0000000-0000-0000-0000-000000000004', 'Security', 'Handles security vulnerabilities, incident response, and compliance', TRUE),
    ('b0000000-0000-0000-0000-000000000005', 'Senior Support', 'Tier 2 escalation team for complex technical and operational issues', TRUE),
    ('b0000000-0000-0000-0000-000000000006', 'Management', 'Executive oversight and high-priority escalation resolution', TRUE)
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

-- =============================================================================
-- 2. REFERENCE CATEGORIES
-- =============================================================================
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

-- =============================================================================
-- 3. REFERENCE SLA POLICIES
-- =============================================================================
INSERT INTO sla_policies (id, name, priority, first_response_minutes, resolution_minutes, is_active) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'Standard Low Priority SLA', 'LOW', 1440, 2880, TRUE),      -- 24h response, 48h resolution
    ('d0000000-0000-0000-0000-000000000002', 'Standard Medium Priority SLA', 'MEDIUM', 480, 1440, TRUE),    -- 8h response, 24h resolution
    ('d0000000-0000-0000-0000-000000000003', 'Standard High Priority SLA', 'HIGH', 120, 480, TRUE),        -- 2h response, 8h resolution
    ('d0000000-0000-0000-0000-000000000004', 'Critical Urgent Priority SLA', 'URGENT', 30, 120, TRUE)      -- 30m response, 2h resolution
ON CONFLICT (priority) DO UPDATE
SET first_response_minutes = EXCLUDED.first_response_minutes,
    resolution_minutes = EXCLUDED.resolution_minutes;

-- =============================================================================
-- 4. REFERENCE ROUTING RULES
-- =============================================================================
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

-- =============================================================================
-- 5. REFERENCE ESCALATION RULES
-- =============================================================================
INSERT INTO escalation_rules (id, name, from_team_id, to_team_id, trigger_type, trigger_value, escalation_level, is_active) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'General to Senior Escalation (Time)', 'b0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', 'TIME', '1440', 1, TRUE),
    ('f0000000-0000-0000-0000-000000000002', 'Technical to Senior Escalation (Priority)', 'b0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000005', 'PRIORITY', 'URGENT', 1, TRUE),
    ('f0000000-0000-0000-0000-000000000003', 'Security to Management Escalation (Urgent)', 'b0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000006', 'PRIORITY', 'URGENT', 2, TRUE)
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 6. DEMO USERS (DEVELOPMENT ONLY)
-- Standard BCrypt Hash for "Password123!":
-- $2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6
-- =============================================================================
INSERT INTO users (id, email, password_hash, role, is_active) VALUES
    -- System Admin
    ('a0000000-0000-0000-0000-000000000001', 'admin@smartdesk.local', '$2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6', 'ADMIN', TRUE),
    -- Support Agents
    ('a0000000-0000-0000-0000-000000000002', 'agent.billing@smartdesk.local', '$2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6', 'AGENT', TRUE),
    ('a0000000-0000-0000-0000-000000000003', 'agent.tech@smartdesk.local', '$2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6', 'AGENT', TRUE),
    ('a0000000-0000-0000-0000-000000000004', 'agent.security@smartdesk.local', '$2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6', 'AGENT', TRUE),
    -- Customers
    ('a0000000-0000-0000-0000-000000000005', 'alex@acmecorp.local', '$2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6', 'CUSTOMER', TRUE),
    ('a0000000-0000-0000-0000-000000000006', 'sara@globex.local', '$2a$12$9AfwxpE4ZoBTGtn8HaYXDe6B/.joOBNR.t7HL./KUhzQykpYYhoQ6', 'CUSTOMER', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Profiles for Demo Users
INSERT INTO profiles (id, user_id, first_name, last_name, phone, avatar_url) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'System', 'Administrator', '+1-555-0100', 'https://avatar.smartdesk.local/admin.png'),
    ('a1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Alice', 'Billington', '+1-555-0102', 'https://avatar.smartdesk.local/alice.png'),
    ('a1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Bob', 'Technician', '+1-555-0103', 'https://avatar.smartdesk.local/bob.png'),
    ('a1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'Sam', 'Securitas', '+1-555-0104', 'https://avatar.smartdesk.local/sam.png'),
    ('a1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 'Alex', 'Acme', '+1-555-0201', 'https://avatar.smartdesk.local/alex.png'),
    ('a1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 'Sara', 'Globex', '+1-555-0202', 'https://avatar.smartdesk.local/sara.png')
ON CONFLICT (user_id) DO NOTHING;

-- Demo Customers
INSERT INTO customers (id, user_id, customer_code, company_name, plan) VALUES
    ('a2000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 'CUST-ACME-001', 'Acme Corporation', 'ENTERPRISE'),
    ('a2000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 'CUST-GLBX-002', 'Globex International', 'PREMIUM')
ON CONFLICT (customer_code) DO NOTHING;

-- Demo Support Agents
INSERT INTO agents (id, user_id, team_id, employee_code, availability_status, skills, max_active_tickets) VALUES
    ('a3000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'EMP-BIL-101', 'AVAILABLE', 'billing, invoicing, payments, stripe', 10),
    ('a3000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'EMP-TEC-102', 'AVAILABLE', 'java, spring, postgresql, nextjs, api', 8),
    ('a3000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'EMP-SEC-103', 'BUSY', 'tls, oauth2, jwt, cve, threat-modelling', 5)
ON CONFLICT (employee_code) DO NOTHING;

-- =============================================================================
-- 7. DEMO TICKETS & OPERATIONAL DATA
-- =============================================================================
INSERT INTO tickets (
    id, ticket_number, customer_id, category_id, assigned_agent_id, assigned_team_id,
    subject, description, priority, status, ai_category, ai_confidence, ai_model_version
) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'TICK-1001',
        'a2000000-0000-0000-0000-000000000005', -- Acme Corp
        'c0000000-0000-0000-0000-000000000001', -- BILLING
        'a3000000-0000-0000-0000-000000000002', -- Alice Billington
        'b0000000-0000-0000-0000-000000000002', -- Billing Team
        'Discrepancy on March Enterprise Invoice',
        'We noticed an extra seat charge on our monthly invoice #INV-4920. Please review our enterprise discount tier.',
        'MEDIUM',
        'IN_PROGRESS',
        'BILLING',
        0.9650,
        'smartdesk-classifier-v1.0'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'TICK-1002',
        'a2000000-0000-0000-0000-000000000006', -- Globex Int
        'c0000000-0000-0000-0000-000000000007', -- BUG
        'a3000000-0000-0000-0000-000000000003', -- Bob Technician
        'b0000000-0000-0000-0000-000000000003', -- Technical Support Team
        'OAuth2 callback returns HTTP 500 intermittently',
        'Our SSO integration throws HTTP 500 error when exchanging authorization codes on the callback endpoint.',
        'HIGH',
        'OPEN',
        'BUG',
        0.9120,
        'smartdesk-classifier-v1.0'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'TICK-1003',
        'a2000000-0000-0000-0000-000000000005', -- Acme Corp
        'c0000000-0000-0000-0000-000000000005', -- SECURITY
        'a3000000-0000-0000-0000-000000000004', -- Sam Securitas
        'b0000000-0000-0000-0000-000000000004', -- Security Team
        'Suspicious API token usage alert detected',
        'Automated monitoring flagged unexpected IP address calling the admin telemetry endpoint at 03:00 UTC.',
        'URGENT',
        'ESCALATED',
        'SECURITY',
        0.9890,
        'smartdesk-classifier-v1.0'
    )
ON CONFLICT (ticket_number) DO NOTHING;

-- Demo Messages for TICK-1001
INSERT INTO ticket_messages (id, ticket_id, sender_user_id, message, is_internal) VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000005', -- Alex (Customer)
        'We noticed an extra seat charge on our monthly invoice #INV-4920. Please review our enterprise discount tier.',
        FALSE
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000002', -- Alice (Agent)
        'Internal Note: Checked Stripe invoice history. Looks like 1 additional seat was provisioned during mid-cycle.',
        TRUE
    ),
    (
        '20000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000002', -- Alice (Agent)
        'Hello Alex, thank you for reaching out. We are reviewing the mid-cycle prorated seat addition and will adjust if applicable.',
        FALSE
    )
ON CONFLICT (id) DO NOTHING;

-- Demo Ticket Events for TICK-1001 and TICK-1003
INSERT INTO ticket_events (ticket_id, actor_user_id, event_type, old_value, new_value, metadata) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000005',
        'TICKET_CREATED',
        NULL,
        'OPEN',
        '{"source": "CUSTOMER_PORTAL", "ip": "192.168.1.10"}'::jsonb
    ),
    (
        '10000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        'ASSIGNED',
        'UNASSIGNED',
        'b0000000-0000-0000-0000-000000000002',
        '{"routed_by": "AUTOMATIC_ROUTING_RULE", "rule": "Billing Route"}'::jsonb
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'a0000000-0000-0000-0000-000000000004',
        'ESCALATED',
        'OPEN',
        'ESCALATED',
        '{"reason": "Potential credential exfiltration", "level": 2}'::jsonb
    );

-- Demo Ticket Assignment History
INSERT INTO ticket_assignments (ticket_id, agent_id, team_id, assigned_by_user_id, assignment_reason) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'a3000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001',
        'Initial automated assignment by Category routing rule'
    );

-- Demo Escalation Record for TICK-1003
INSERT INTO escalations (
    ticket_id, from_team_id, to_team_id, from_agent_id, to_agent_id, level, reason, status
) VALUES
    (
        '10000000-0000-0000-0000-000000000003',
        'b0000000-0000-0000-0000-000000000004', -- Security Team
        'b0000000-0000-0000-0000-000000000006', -- Management Team
        'a3000000-0000-0000-0000-000000000004', -- Sam Securitas
        NULL,
        2,
        'Critical security event requires executive briefing and temporary API key revocation authorization',
        'OPEN'
    );

-- Demo Notifications
INSERT INTO notifications (user_id, ticket_id, type, title, message, is_read) VALUES
    (
        'a0000000-0000-0000-0000-000000000005', -- Alex (Customer)
        '10000000-0000-0000-0000-000000000001',
        'TICKET_STATUS_UPDATED',
        'Ticket TICK-1001 In Progress',
        'Your billing inquiry has been picked up by Alice from Billing Support.',
        FALSE
    ),
    (
        'a0000000-0000-0000-0000-000000000001', -- Admin
        '10000000-0000-0000-0000-000000000003',
        'SECURITY_ALERT',
        'Urgent Escalation: TICK-1003',
        'Ticket TICK-1003 escalated to Management due to suspicious API activity.',
        FALSE
    );

COMMIT;
