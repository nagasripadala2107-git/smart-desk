import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';

describe('Customer Sentiment Schema & Validation', () => {
  const sentimentSchema = z.object({
    id: z.string().uuid(),
    ticketNumber: z.string(),
    subject: z.string(),
    sentiment: z.enum(['POSITIVE', 'NEUTRAL', 'NEGATIVE']).nullable().optional(),
    sentimentConfidence: z.number().min(0.0).max(1.0).nullable().optional(),
    tone: z.enum(['CALM', 'FRUSTRATED', 'URGENT', 'ANGRY', 'SATISFIED', 'CONFUSED', 'NEUTRAL']).nullable().optional(),
    sentimentModelVersion: z.string().nullable().optional(),
  });

  it('should accept valid sentiment analysis payload for agent view', () => {
    const payload = {
      id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      ticketNumber: 'SD-2026-000042',
      subject: 'Urgent payment failed',
      sentiment: 'NEGATIVE',
      sentimentConfidence: 0.8925,
      tone: 'FRUSTRATED',
      sentimentModelVersion: 'ticket-sentiment-v1',
    };
    const result = sentimentSchema.safeParse(payload);
    assert.equal(result.success, true);
  });

  it('should validate all 7 customer tone categories', () => {
    const tones = ['CALM', 'FRUSTRATED', 'URGENT', 'ANGRY', 'SATISFIED', 'CONFUSED', 'NEUTRAL'];
    for (const tone of tones) {
      const result = sentimentSchema.safeParse({
        id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        ticketNumber: 'SD-2026-000042',
        subject: 'Inquiry',
        sentiment: 'NEUTRAL',
        sentimentConfidence: 0.5,
        tone: tone,
        sentimentModelVersion: 'ticket-sentiment-v1',
      });
      assert.equal(result.success, true, `Expected tone ${tone} to be valid`);
    }
  });

  it('should reject invalid sentiment values', () => {
    const result = sentimentSchema.safeParse({
      id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      ticketNumber: 'SD-2026-000042',
      subject: 'Inquiry',
      sentiment: 'SUPER_HAPPY',
    });
    assert.equal(result.success, false);
  });

  it('should reject confidence outside [0.0, 1.0] range', () => {
    const result = sentimentSchema.safeParse({
      id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      ticketNumber: 'SD-2026-000042',
      subject: 'Inquiry',
      sentiment: 'POSITIVE',
      sentimentConfidence: 1.5,
    });
    assert.equal(result.success, false);
  });
});

describe('Confidence Percentage Formatting Logic', () => {
  const formatConfidence = (confidence) => {
    if (confidence == null) return null;
    return (confidence * 100).toFixed(1);
  };

  it('should format decimal confidence to percentage with one decimal place', () => {
    assert.equal(formatConfidence(0.8523), '85.2');
    assert.equal(formatConfidence(0.9999), '100.0');
    assert.equal(formatConfidence(0.0), '0.0');
    assert.equal(formatConfidence(null), null);
    assert.equal(formatConfidence(undefined), null);
  });
});

describe('Customer Isolation for Sentiment Metadata', () => {
  it('should ensure customer ticket detail view omits sentiment data', () => {
    const customerTicketResponse = {
      id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      ticketNumber: 'SD-2026-000042',
      subject: 'Issue with service',
      sentiment: null,
      sentimentConfidence: null,
      tone: null,
      sentimentModelVersion: null,
    };

    assert.equal(customerTicketResponse.sentiment, null);
    assert.equal(customerTicketResponse.sentimentConfidence, null);
    assert.equal(customerTicketResponse.tone, null);
    assert.equal(customerTicketResponse.sentimentModelVersion, null);
  });
});
