'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketTable } from '@/components/tickets/TicketTable';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { EscalationPathDiagram } from '@/components/escalation/EscalationPathDiagram';
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

      {/* Visual Escalation Path Architecture */}
      <EscalationPathDiagram />

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
