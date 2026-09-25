import React from 'react';
import { AnalyticsOverviewResponse } from '@/types';
import { StatCard } from '@/components/common/StatCard';
import {
  Ticket,
  Clock,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Archive,
  Hourglass,
  Timer,
} from 'lucide-react';

interface OverviewKpiCardsProps {
  overview: AnalyticsOverviewResponse;
}

export function OverviewKpiCards({ overview }: OverviewKpiCardsProps) {
  const formatDuration = (mins?: number | null) => {
    if (mins == null) return 'N/A';
    if (mins < 60) return `${Math.round(mins)} min`;
    const hours = (mins / 60).toFixed(1);
    return `${hours} hrs`;
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
        <StatCard
          title="Total Tickets"
          value={overview.totalTickets}
          icon={<Ticket className="w-5 h-5" />}
          description="All operational tickets"
        />
        <StatCard
          title="Open"
          value={overview.openTickets}
          icon={<Clock className="w-5 h-5 text-amber-500" />}
          description="Awaiting response"
        />
        <StatCard
          title="In Progress"
          value={overview.inProgressTickets}
          icon={<RefreshCw className="w-5 h-5 text-blue-500" />}
          description="Actively investigated"
        />
        <StatCard
          title="Escalated"
          value={overview.escalatedTickets}
          icon={<AlertTriangle className="w-5 h-5 text-rose-500" />}
          description="Higher-tier tickets"
        />
        <StatCard
          title="Resolved"
          value={overview.resolvedTickets}
          icon={<CheckCircle2 className="w-5 h-5 text-emerald-500" />}
          description="Awaiting closure"
        />
        <StatCard
          title="Closed"
          value={overview.closedTickets}
          icon={<Archive className="w-5 h-5 text-slate-500" />}
          description="Completed lifecycle"
        />
      </div>

      {/* Secondary Operational Metrics */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30 text-xs text-slate-600 dark:text-slate-400">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1.5">
            <Timer className="w-4 h-4 text-indigo-500" />
            <strong className="text-slate-900 dark:text-slate-200">Avg Resolution Time:</strong>{' '}
            {formatDuration(overview.avgResolutionMinutes)}
          </span>
          <span className="text-slate-300 dark:text-slate-700">|</span>
          <span className="flex items-center gap-1.5">
            <Hourglass className="w-4 h-4 text-amber-500" />
            <strong className="text-slate-900 dark:text-slate-200">Pending Tickets:</strong>{' '}
            {overview.pendingTickets}
          </span>
        </div>
        <div className="text-[11px] text-slate-400">
          Operational metrics backed by PostgreSQL
        </div>
      </div>
    </div>
  );
}
