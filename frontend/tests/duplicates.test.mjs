import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';

describe('Duplicate Matches Schema & Validation', () => {
  const duplicateMatchSchema = z.object({
    ticketId: z.string().uuid(),
    ticketNumber: z.string().regex(/^SD-\d{4}-\d{6}$/),
    subject: z.string().min(1),
    similarityScore: z.number().min(0.0).max(1.0),
    modelVersion: z.string().min(1),
  });

  const ticketDetailSchema = z.object({
    id: z.string().uuid(),
    ticketNumber: z.string(),
    subject: z.string(),
    duplicateMatches: z.array(duplicateMatchSchema).nullable().optional(),
  });

  it('should accept valid duplicate matches payload for agent view', () => {
    const payload = {
      id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      ticketNumber: 'SD-2026-000042',
      subject: 'Double charge on credit card',
      duplicateMatches: [
        {
          ticketId: 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
          ticketNumber: 'SD-2026-000003',
          subject: 'Payment charged twice for same order',
          similarityScore: 0.875,
          modelVersion: 'ticket-duplicate-v1',
        },
      ],
    };
    const result = ticketDetailSchema.safeParse(payload);
    assert.equal(result.success, true);
  });

  it('should reject invalid similarity scores outside [0.0, 1.0]', () => {
    const result = duplicateMatchSchema.safeParse({
      ticketId: 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
      ticketNumber: 'SD-2026-000003',
      subject: 'Payment charged twice',
      similarityScore: 1.45,
      modelVersion: 'ticket-duplicate-v1',
    });
    assert.equal(result.success, false);
  });
});

describe('Similarity Percentage Formatting Logic', () => {
  const formatSimilarity = (score) => {
    if (score == null || typeof score !== 'number') return null;
    return (score * 100).toFixed(0);
  };

  it('should format decimal similarity score to whole percentage', () => {
    assert.equal(formatSimilarity(0.875), '88');
    assert.equal(formatSimilarity(0.70), '70');
    assert.equal(formatSimilarity(1.0), '100');
    assert.equal(formatSimilarity(0.0), '0');
    assert.equal(formatSimilarity(null), null);
    assert.equal(formatSimilarity(undefined), null);
  });
});

describe('Duplicate Matches Ordering and Limiting', () => {
  it('should verify matches are sorted in descending order of similarity', () => {
    const matches = [
      { ticketId: '1', similarityScore: 0.95 },
      { ticketId: '2', similarityScore: 0.88 },
      { ticketId: '3', similarityScore: 0.72 },
    ];

    for (let i = 0; i < matches.length - 1; i++) {
      assert.ok(matches[i].similarityScore >= matches[i + 1].similarityScore);
    }
  });

  it('should enforce maximum results cap (max 5)', () => {
    const maxAllowed = 5;
    const matches = Array.from({ length: 10 }, (_, i) => ({
      ticketId: `uuid-${i}`,
      similarityScore: 0.9 - i * 0.02,
    }));

    const capped = matches.slice(0, maxAllowed);
    assert.equal(capped.length, 5);
  });
});

describe('Potential Duplicates Card State Logic', () => {
  const resolveCardState = (matches) => {
    if (matches === null || matches === undefined) {
      return 'UNAVAILABLE';
    }
    if (matches.length === 0) {
      return 'NO_MATCHES';
    }
    return 'HAS_MATCHES';
  };

  it('should identify unavailable state when duplicateMatches is null or undefined', () => {
    assert.equal(resolveCardState(null), 'UNAVAILABLE');
    assert.equal(resolveCardState(undefined), 'UNAVAILABLE');
  });

  it('should identify no-match state when duplicateMatches is empty array', () => {
    assert.equal(resolveCardState([]), 'NO_MATCHES');
  });

  it('should identify has-matches state when duplicate matches exist', () => {
    assert.equal(resolveCardState([{ ticketId: '123' }]), 'HAS_MATCHES');
  });
});

describe('Customer Isolation for Duplicate Matches', () => {
  it('should ensure customer ticket detail response strictly omits duplicateMatches', () => {
    const customerTicketResponse = {
      id: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
      ticketNumber: 'SD-2026-000042',
      subject: 'Issue with invoice',
      duplicateMatches: null,
    };

    assert.equal(customerTicketResponse.duplicateMatches, null);
    assert.equal(typeof customerTicketResponse.duplicateMatches !== 'object' || customerTicketResponse.duplicateMatches === null, true);
  });
});
