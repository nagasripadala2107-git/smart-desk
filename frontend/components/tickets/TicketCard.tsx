import React from 'react';
import Link from 'next/link';
import { TicketSummaryResponse } from '@/types';
import { Card, CardContent } from '@/components/ui/Card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { PriorityBadge } from '@/components/common/PriorityBadge';
import { formatRelativeTime } from '@/lib/utils';

interface TicketCardProps {
  ticket: TicketSummaryResponse;
  basePath?: string;
}

export function TicketCard({ ticket, basePath = '/customer/tickets' }: TicketCardProps) {
  return (
    <Card className="hover:border-indigo-200 dark:hover:border-indigo-800 transition-colors">
      <CardContent className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <span className="font-mono text-xs font-semibold text-indigo-600 dark:text-indigo-400">
            {ticket.ticketNumber}
          </span>
          <div className="flex items-center gap-1.5">
            <PriorityBadge priority={ticket.priority} />
            <StatusBadge status={ticket.status} />
          </div>
        </div>

        <div>
          <Link
            href={`${basePath}/${ticket.id}`}
            className="text-sm font-semibold text-slate-900 dark:text-slate-100 hover:text-indigo-600 dark:hover:text-indigo-400 line-clamp-1"
          >
            {ticket.subject}
          </Link>
          <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 mt-2">
            <span>{ticket.categoryName || 'General Support'}</span>
            <span>{formatRelativeTime(ticket.createdAt)}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
