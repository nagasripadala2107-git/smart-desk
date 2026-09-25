'use client';

import React, { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketTable } from '@/components/tickets/TicketTable';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterBar } from '@/components/common/FilterBar';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { Button } from '@/components/ui/Button';
import { PlusCircle } from 'lucide-react';

export default function CustomerTicketsPage() {
  const router = useRouter();
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

  const filteredTickets = useMemo(() => {
    return tickets.filter((t) => {
      const matchesSearch =
        !search ||
        t.ticketNumber.toLowerCase().includes(search.toLowerCase()) ||
        t.subject.toLowerCase().includes(search.toLowerCase()) ||
        (t.categoryName && t.categoryName.toLowerCase().includes(search.toLowerCase()));

      const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
      const matchesPriority = priorityFilter === 'ALL' || t.priority === priorityFilter;

      return matchesSearch && matchesStatus && matchesPriority;
    });
  }, [tickets, search, statusFilter, priorityFilter]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Support Tickets"
        description="Browse all submitted tickets, filter by status or priority, and check progress."
        actions={
          <Link href="/customer/tickets/new">
            <Button size="sm" className="gap-1.5 shadow-sm">
              <PlusCircle className="w-4 h-4" />
              Create Ticket
            </Button>
          </Link>
        }
      />

      {/* Search and Filters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-xs">
        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Search by ticket ID, subject, or category..."
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
          title="No tickets found"
          description="You have not submitted any support tickets yet."
          actionLabel="Create a Ticket"
          onAction={() => router.push('/customer/tickets/new')}
        />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          title="No matching tickets"
          description="No tickets match your current filter and search criteria."
          actionLabel="Clear Filters"
          onAction={() => {
            setSearch('');
            setStatusFilter('ALL');
            setPriorityFilter('ALL');
          }}
        />
      ) : (
        <TicketTable tickets={filteredTickets} basePath="/customer/tickets" />
      )}
    </div>
  );
}
