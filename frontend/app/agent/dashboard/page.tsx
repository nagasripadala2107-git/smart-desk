'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import { AgentQueueResponse, TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard } from '@/components/common/StatCard';
import { TicketTable } from '@/components/tickets/TicketTable';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import {
  Ticket,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Users,
  ArrowRight,
  Flame,
} from 'lucide-react';

export default function AgentDashboardPage() {
  const [queue, setQueue] = useState<AgentQueueResponse | null>(null);
  const [allMyTickets, setAllMyTickets] = useState<TicketSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAgentData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [queueData, ticketsData] = await Promise.all([
        api.get<AgentQueueResponse>('/agent/queue'),
        api.get<TicketSummaryResponse[]>('/agent/tickets'),
      ]);
      setQueue(queueData);
      setAllMyTickets(ticketsData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load agent queue');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAgentData();
  }, []);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <LoadingSkeleton type="card" />
        <LoadingSkeleton rows={5} />
      </div>
    );
  }

  if (error || !queue) {
    return (
      <div className="py-12">
        <ErrorState
          title="Agent Workspace Unavailable"
          message={error || 'Unable to connect to agent queue'}
          onRetry={fetchAgentData}
        />
      </div>
    );
  }

  const assignedTickets = queue.assignedTickets || [];
  const teamTickets = queue.teamQueueTickets || [];

  const openTickets = assignedTickets.filter(
    (t) => t.status !== 'RESOLVED' && t.status !== 'CLOSED'
  );
  const escalatedTickets = assignedTickets.filter((t) => t.status === 'ESCALATED');
  const resolvedTickets = allMyTickets.filter(
    (t) => t.status === 'RESOLVED' || t.status === 'CLOSED'
  );
  const priorityTickets = assignedTickets.filter(
    (t) => t.priority === 'URGENT' || t.priority === 'HIGH'
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Agent Console — ${queue.agentName}`}
        description={`Team: ${queue.teamName || 'Support Operations'} • Employee Code: ${queue.employeeCode}`}
      />

      {/* Stats row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Open Tickets"
          value={openTickets.length}
          icon={<Ticket className="w-5 h-5" />}
          description="In your active queue"
        />
        <StatCard
          title="Escalated Tickets"
          value={escalatedTickets.length}
          icon={<AlertTriangle className="w-5 h-5" />}
          description="Tier 2/3 active escalations"
        />
        <StatCard
          title="SLA Risk"
          value={priorityTickets.length}
          icon={<Clock className="w-5 h-5" />}
          description="High or urgent priority"
        />
        <StatCard
          title="Resolved"
          value={resolvedTickets.length}
          icon={<CheckCircle2 className="w-5 h-5" />}
          description="Closed or resolved"
        />
      </div>

      {/* Priority Tickets Alert Section (if any urgent) */}
      {priorityTickets.length > 0 && (
        <Card className="border-amber-200 dark:border-amber-900/60 bg-amber-50/40 dark:bg-amber-950/20">
          <CardHeader className="pb-2">
            <div className="flex items-center gap-2 text-amber-800 dark:text-amber-200">
              <Flame className="w-4 h-4 text-rose-500" />
              <CardTitle className="text-sm">High Priority Tickets Requiring Immediate Action</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="pt-2">
            <TicketTable tickets={priorityTickets} basePath="/agent/tickets" showCustomer={true} />
          </CardContent>
        </Card>
      )}

      {/* My Queue Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              My Queue ({assignedTickets.length})
            </h2>
          </div>
          <Link
            href="/agent/tickets"
            className="text-xs font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 flex items-center gap-1"
          >
            Manage Queue
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {assignedTickets.length === 0 ? (
          <EmptyState
            title="Queue is empty"
            description="You currently have no tickets directly assigned to you."
          />
        ) : (
          <TicketTable tickets={assignedTickets} basePath="/agent/tickets" showCustomer={true} />
        )}
      </div>

      {/* Team Queue Section */}
      <div className="space-y-3 pt-4 border-t border-slate-100 dark:border-slate-800">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Users className="w-4 h-4 text-slate-400" />
            <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              {queue.teamName || 'Team'} Unassigned Queue ({teamTickets.length})
            </h2>
          </div>
        </div>

        {teamTickets.length === 0 ? (
          <EmptyState
            title="No unassigned team tickets"
            description="All tickets in your team's pool have been picked up."
          />
        ) : (
          <TicketTable tickets={teamTickets} basePath="/agent/tickets" showCustomer={true} />
        )}
      </div>
    </div>
  );
}
