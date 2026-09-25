import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';

// ==========================================
// 1. Analytics Overview & KPI Formatting
// ==========================================
describe('Analytics Overview & KPI Formatting', () => {
  const formatDuration = (mins) => {
    if (mins == null) return 'N/A';
    if (mins < 60) return `${Math.round(mins)} min`;
    const hours = (mins / 60).toFixed(1);
    return `${hours} hrs`;
  };

  it('should format minutes correctly under 60 mins', () => {
    assert.equal(formatDuration(45), '45 min');
    assert.equal(formatDuration(0), '0 min');
  });

  it('should format hours correctly over 60 mins', () => {
    assert.equal(formatDuration(120), '2.0 hrs');
    assert.equal(formatDuration(90), '1.5 hrs');
  });

  it('should handle null or undefined duration gracefully', () => {
    assert.equal(formatDuration(null), 'N/A');
    assert.equal(formatDuration(undefined), 'N/A');
  });

  const overviewSchema = z.object({
    totalTickets: z.number().min(0),
    openTickets: z.number().min(0),
    inProgressTickets: z.number().min(0),
    pendingTickets: z.number().min(0),
    escalatedTickets: z.number().min(0),
    resolvedTickets: z.number().min(0),
    closedTickets: z.number().min(0),
    avgResolutionMinutes: z.number().nullable().optional(),
  });

  it('should validate complete overview response schema', () => {
    const payload = {
      totalTickets: 9,
      openTickets: 4,
      inProgressTickets: 1,
      pendingTickets: 0,
      escalatedTickets: 2,
      resolvedTickets: 0,
      closedTickets: 2,
      avgResolutionMinutes: 1.6,
    };
    const res = overviewSchema.safeParse(payload);
    assert.equal(res.success, true);
  });
});

// ==========================================
// 2. Ticket Volume Over Time
// ==========================================
describe('Ticket Volume Over Time Logic', () => {
  const volumeItemSchema = z.object({
    date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
    count: z.number().min(0),
  });

  it('should validate volume payload and ensure non-negative counts', () => {
    const data = [
      { date: '2026-09-18', count: 0 },
      { date: '2026-09-19', count: 5 },
      { date: '2026-09-20', count: 4 },
    ];
    for (const item of data) {
      assert.equal(volumeItemSchema.safeParse(item).success, true);
    }
  });

  it('should verify continuous date range generation', () => {
    const days = 7;
    const dates = [];
    const end = new Date('2026-09-20T00:00:00Z');
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(end);
      d.setDate(d.getDate() - i);
      dates.push(d.toISOString().slice(0, 10));
    }
    assert.equal(dates.length, 7);
    assert.equal(dates[0], '2026-09-14');
    assert.equal(dates[6], '2026-09-20');
  });
});

// ==========================================
// 3. Team Workload & Agent Metrics Factual Integrity
// ==========================================
describe('Team Workload & Agent Metrics Schema', () => {
  const teamWorkloadSchema = z.object({
    teamId: z.string().regex(/^[0-9a-fA-F-]{36}$/),
    teamName: z.string().min(1),
    activeTickets: z.number().min(0),
    resolvedClosedTickets: z.number().min(0),
    escalatedTickets: z.number().min(0),
  });

  const agentPerformanceSchema = z.object({
    agentId: z.string().regex(/^[0-9a-fA-F-]{36}$/),
    agentName: z.string().min(1),
    employeeCode: z.string().min(1),
    teamName: z.string().min(1),
    assignedTickets: z.number().min(0),
    openTickets: z.number().min(0),
    resolvedTickets: z.number().min(0),
    avgResolutionMinutes: z.number().nullable().optional(),
  });

  it('should validate factual team workload data without subjective scores', () => {
    const team = {
      teamId: 'b0000000-0000-0000-0000-000000000001',
      teamName: 'General Support',
      activeTickets: 3,
      resolvedClosedTickets: 2,
      escalatedTickets: 1,
    };
    assert.equal(teamWorkloadSchema.safeParse(team).success, true);
  });

  it('should validate agent operational metrics without rankings', () => {
    const agent = {
      agentId: 'a3000000-0000-0000-0000-000000000002',
      agentName: 'Alice Agent',
      employeeCode: 'EMP-BIL-101',
      teamName: 'Billing',
      assignedTickets: 4,
      openTickets: 2,
      resolvedTickets: 2,
      avgResolutionMinutes: 45.2,
    };
    assert.equal(agentPerformanceSchema.safeParse(agent).success, true);
  });
});

// ==========================================
// 4. Escalations & SLA Metrics
// ==========================================
describe('Escalations & SLA Metrics Schema', () => {
  const slaSchema = z.object({
    configuredPoliciesCount: z.number().min(0),
    ticketsPastResponseTargetCount: z.number().min(0),
    ticketsPastResolutionTargetCount: z.number().min(0),
    policies: z.array(z.object({
      id: z.string().regex(/^[0-9a-fA-F-]{36}$/),
      name: z.string(),
      priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
      firstResponseMinutes: z.number(),
      resolutionMinutes: z.number(),
      isActive: z.boolean(),
    })),
  });

  it('should validate SLA schema representing measurable thresholds', () => {
    const slaData = {
      configuredPoliciesCount: 4,
      ticketsPastResponseTargetCount: 2,
      ticketsPastResolutionTargetCount: 1,
      policies: [
        {
          id: 'd0000000-0000-0000-0000-000000000001',
          name: 'Standard High Priority SLA',
          priority: 'HIGH',
          firstResponseMinutes: 120,
          resolutionMinutes: 480,
          isActive: true,
        },
      ],
    };
    assert.equal(slaSchema.safeParse(slaData).success, true);
  });
});
