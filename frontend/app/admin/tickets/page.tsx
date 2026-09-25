'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { api } from '@/lib/api';
import { TicketSummaryResponse, CategoryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketTable } from '@/components/tickets/TicketTable';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterBar } from '@/components/common/FilterBar';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';

export default function AdminTicketsPage() {
  const [tickets, setTickets] = useState<TicketSummaryResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');

  const fetchTickets = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [ticketsData, catsData] = await Promise.all([
        api.get<TicketSummaryResponse[]>('/tickets'),
        api.get<CategoryResponse[]>('/categories').catch(() => []),
      ]);
      setTickets(ticketsData);
      setCategories(catsData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to retrieve tickets');
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
        (t.assignedAgentName && t.assignedAgentName.toLowerCase().includes(search.toLowerCase()));

      const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
      const matchesPriority = priorityFilter === 'ALL' || t.priority === priorityFilter;
      const matchesCategory =
        categoryFilter === 'ALL' ||
        categories.find((c) => c.id === categoryFilter)?.name === t.categoryName;

      return matchesSearch && matchesStatus && matchesPriority && matchesCategory;
    });
  }, [tickets, search, statusFilter, priorityFilter, categoryFilter, categories]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Global Ticket Repository"
        description="Comprehensive view of all customer tickets across all support teams and tiers."
      />

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
          showCategoryFilter={true}
          categoryFilter={categoryFilter}
          onCategoryChange={setCategoryFilter}
          categories={categories}
          onReset={() => {
            setSearch('');
            setStatusFilter('ALL');
            setPriorityFilter('ALL');
            setCategoryFilter('ALL');
          }}
        />
      </div>

      {isLoading ? (
        <LoadingSkeleton rows={6} />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchTickets} />
      ) : tickets.length === 0 ? (
        <EmptyState
          title="No tickets in repository"
          description="There are currently no tickets registered in the system."
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
            setCategoryFilter('ALL');
          }}
        />
      ) : (
        <TicketTable tickets={filteredTickets} basePath="/agent/tickets" showCustomer={true} />
      )}
    </div>
  );
}
