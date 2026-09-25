'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { api } from '@/lib/api';
import { TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketTable } from '@/components/tickets/TicketTable';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterBar } from '@/components/common/FilterBar';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';

export default function AgentTicketsPage() {
  const [tickets, setTickets] = useState<TicketSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');

  const fetchTickets = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.get<TicketSummaryResponse[]>('/agent/tickets');
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

  const filteredTickets = useMemo(() => {
    return tickets.filter((t) => {
      const matchesSearch =
        !search ||
        t.ticketNumber.toLowerCase().includes(search.toLowerCase()) ||
        t.subject.toLowerCase().includes(search.toLowerCase()) ||
        (t.customerName && t.customerName.toLowerCase().includes(search.toLowerCase())) ||
        (t.categoryName && t.categoryName.toLowerCase().includes(search.toLowerCase()));

      const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
      const matchesPriority = priorityFilter === 'ALL' || t.priority === priorityFilter;

      return matchesSearch && matchesStatus && matchesPriority;
    });
  }, [tickets, search, statusFilter, priorityFilter]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Agent Ticket Queue"
        description="All tickets assigned to your workload. Filter by status or priority to prioritize responses."
      />

      {/* Search and Filters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-xs">
        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Search by ticket ID, subject, or customer..."
        />
        <FilterBar
          statusFilter={statusFilter}
          onStatusChange={setStatusFilter}
          priorityFilter={priorityFilter}
          onPriorityChange={setPriorityFilter}
          onReset={() => {
            setSearch('');
            setStatusFilter('ALL');
            setPriorityFilter('ALL');
          }}
        />
      </div>

      {isLoading ? (
        <LoadingSkeleton rows={6} />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchTickets} />
      ) : tickets.length === 0 ? (
        <EmptyState
          title="No assigned tickets"
          description="Your queue is currently clear. Check the team queue on your dashboard to pick up unassigned tickets."
        />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          title="No matching tickets"
          description="No tickets match your filter criteria."
          actionLabel="Clear Filters"
          onAction={() => {
            setSearch('');
            setStatusFilter('ALL');
            setPriorityFilter('ALL');
          }}
        />
      ) : (
        <TicketTable tickets={filteredTickets} basePath="/agent/tickets" showCustomer={true} />
      )}
    </div>
  );
}
