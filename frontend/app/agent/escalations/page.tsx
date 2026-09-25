'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketTable } from '@/components/tickets/TicketTable';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { AlertOctagon, ShieldAlert } from 'lucide-react';

export default function AgentEscalationsPage() {
  const [escalatedTickets, setEscalatedTickets] = useState<TicketSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEscalations = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const allTickets = await api.get<TicketSummaryResponse[]>('/tickets');
      const escalated = allTickets.filter((t) => t.status === 'ESCALATED');
      setEscalatedTickets(escalated);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load escalated tickets');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEscalations();
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Escalations Management"
        description="Active multi-tier escalations requiring specialized engineering or management intervention."
      />

      <div className="p-4 rounded-xl border border-rose-200 dark:border-rose-900/60 bg-rose-50/40 dark:bg-rose-950/20 flex items-start gap-3">
        <ShieldAlert className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
        <div className="text-xs text-rose-800 dark:text-rose-300 leading-relaxed">
          <strong className="font-semibold">Deterministic Escalation Tracking:</strong> Escalations
          transfer ownership to target engineering tiers with timestamped audit trails. (Interactive
          graph visualization is scheduled for Phase 7).
        </div>
      </div>

      {isLoading ? (
        <LoadingSkeleton rows={5} />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchEscalations} />
      ) : escalatedTickets.length === 0 ? (
        <EmptyState
          title="No active escalations"
          description="There are currently no tickets in ESCALATED status across the active queues."
          icon={<AlertOctagon className="w-6 h-6 text-emerald-500" />}
        />
      ) : (
        <TicketTable tickets={escalatedTickets} basePath="/agent/tickets" showCustomer={true} />
      )}
    </div>
  );
}
