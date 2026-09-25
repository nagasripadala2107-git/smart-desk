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
