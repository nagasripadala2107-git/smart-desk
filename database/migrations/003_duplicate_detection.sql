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
