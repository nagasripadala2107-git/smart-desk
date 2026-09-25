import React from 'react';
import { TicketPriority, TicketStatus } from '@/types';
import { Select } from '@/components/ui/Select';
import { Filter } from 'lucide-react';

interface FilterBarProps {
  statusFilter: string;
  onStatusChange: (status: string) => void;
  priorityFilter: string;
  onPriorityChange: (priority: string) => void;
  showCategoryFilter?: boolean;
  categoryFilter?: string;
  onCategoryChange?: (category: string) => void;
  categories?: { id: string; name: string }[];
  onReset?: () => void;
}

export function FilterBar({
  statusFilter,
  onStatusChange,
  priorityFilter,
  onPriorityChange,
  showCategoryFilter,
  categoryFilter = 'ALL',
  onCategoryChange,
  categories = [],
  onReset,
}: FilterBarProps) {
  const statusOptions: { value: string; label: string }[] = [
    { value: 'ALL', label: 'All Statuses' },
    { value: 'NEW', label: 'New' },
    { value: 'ASSIGNED', label: 'Assigned' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'PENDING_CUSTOMER', label: 'Pending Customer' },
    { value: 'RESOLVED', label: 'Resolved' },
    { value: 'CLOSED', label: 'Closed' },
    { value: 'ESCALATED', label: 'Escalated' },
  ];

  const priorityOptions: { value: string; label: string }[] = [
    { value: 'ALL', label: 'All Priorities' },
    { value: 'LOW', label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH', label: 'High' },
    { value: 'URGENT', label: 'Urgent' },
  ];

  const isFiltered = statusFilter !== 'ALL' || priorityFilter !== 'ALL' || (showCategoryFilter && categoryFilter !== 'ALL');

  return (
    <div className="flex flex-wrap items-center gap-2.5">
      <div className="flex items-center text-xs font-medium text-slate-500 dark:text-slate-400 gap-1 mr-1">
        <Filter className="w-3.5 h-3.5" />
        Filters:
      </div>

      <div className="w-36 sm:w-40">
        <Select
          value={statusFilter}
          onChange={(e) => onStatusChange(e.target.value as TicketStatus | 'ALL')}
          options={statusOptions}
        />
      </div>

      <div className="w-32 sm:w-36">
        <Select
          value={priorityFilter}
          onChange={(e) => onPriorityChange(e.target.value as TicketPriority | 'ALL')}
          options={priorityOptions}
        />
      </div>

      {showCategoryFilter && onCategoryChange && (
        <div className="w-36 sm:w-40">
          <Select
            value={categoryFilter}
            onChange={(e) => onCategoryChange(e.target.value)}
            options={[
              { value: 'ALL', label: 'All Categories' },
              ...categories.map((c) => ({ value: c.id, label: c.name })),
            ]}
          />
        </div>
      )}

      {isFiltered && onReset && (
        <button
          onClick={onReset}
          className="text-xs text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 hover:underline px-1.5 py-1"
        >
          Clear filters
        </button>
      )}
    </div>
  );
}
