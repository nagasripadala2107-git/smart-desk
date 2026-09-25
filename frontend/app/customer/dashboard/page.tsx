'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard } from '@/components/common/StatCard';
import { TicketTable } from '@/components/tickets/TicketTable';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { Button } from '@/components/ui/Button';
import {
  Ticket,
  Clock,
  CheckCircle2,
  AlertTriangle,
  PlusCircle,
  ArrowRight,
} from 'lucide-react';

export default function CustomerDashboardPage() {
  const router = useRouter();
  const [tickets, setTickets] = useState<TicketSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTickets = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.get<TicketSummaryResponse[]>('/customer/tickets');
      setTickets(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load tickets');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTickets();
  }, []);

  const openCount = tickets.filter(
    (t) => t.status === 'NEW' || t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS'
  ).length;
  const pendingCount = tickets.filter((t) => t.status === 'PENDING_CUSTOMER').length;
  const resolvedCount = tickets.filter(
    (t) => t.status === 'RESOLVED' || t.status === 'CLOSED'
  ).length;
  const escalatedCount = tickets.filter((t) => t.status === 'ESCALATED').length;

  const recentTickets = [...tickets]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Customer Support Dashboard"
        description="Track your support requests, status updates, and resolutions in real time."
        actions={
          <Link href="/customer/tickets/new">
            <Button size="sm" className="gap-1.5 shadow-sm">
              <PlusCircle className="w-4 h-4" />
              Create Ticket
            </Button>
          </Link>
        }
      />

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Open Tickets"
          value={openCount}
          icon={<Ticket className="w-5 h-5" />}
          description="In progress or awaiting triage"
        />
        <StatCard
          title="Pending Your Action"
          value={pendingCount}
          icon={<Clock className="w-5 h-5" />}
          description="Agent waiting for your reply"
        />
        <StatCard
          title="Resolved Tickets"
          value={resolvedCount}
          icon={<CheckCircle2 className="w-5 h-5" />}
          description="Completed or closed"
        />
        <StatCard
          title="Escalated"
          value={escalatedCount}
          icon={<AlertTriangle className="w-5 h-5" />}
          description="Transferred to specialist tier"
        />
      </div>

      {/* Recent Tickets Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
            Recent Tickets
          </h2>
          {tickets.length > 5 && (
            <Link
              href="/customer/tickets"
              className="text-xs font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 flex items-center gap-1"
            >
              View all tickets
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          )}
        </div>

        {isLoading ? (
          <LoadingSkeleton rows={4} />
        ) : error ? (
          <ErrorState
            title="Failed to load recent tickets"
            message={error}
            onRetry={fetchTickets}
          />
        ) : recentTickets.length === 0 ? (
          <EmptyState
            title="No support tickets yet"
            description="You haven't submitted any support requests. If you are experiencing an issue, create a new ticket."
            actionLabel="Create your first ticket"
            onAction={() => router.push('/customer/tickets/new')}
          />
        ) : (
          <TicketTable tickets={recentTickets} basePath="/customer/tickets" />
        )}
      </div>
    </div>
  );
}
