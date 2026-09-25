'use client';

import React from 'react';
import Link from 'next/link';
import { TicketSummaryResponse } from '@/types';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatusBadge } from '@/components/common/StatusBadge';
import { PriorityBadge } from '@/components/common/PriorityBadge';
import { formatDate } from '@/lib/utils';
import { ArrowRight, Eye } from 'lucide-react';

interface TicketTableProps {
  tickets: TicketSummaryResponse[];
  basePath?: string; // '/customer/tickets' | '/agent/tickets' | '/admin/tickets'
  showCustomer?: boolean;
}

export function TicketTable({
  tickets,
  basePath = '/customer/tickets',
  showCustomer = false,
}: TicketTableProps) {
  return (
    <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden shadow-xs">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-28">Ticket ID</TableHead>
            <TableHead>Subject</TableHead>
            {showCustomer && <TableHead className="hidden md:table-cell">Customer</TableHead>}
            <TableHead className="hidden sm:table-cell">Category</TableHead>
            <TableHead className="w-28">Priority</TableHead>
            <TableHead className="w-28">Status</TableHead>
            <TableHead className="hidden lg:table-cell">Assigned Team</TableHead>
            <TableHead className="hidden lg:table-cell">Created</TableHead>
            <TableHead className="text-right w-16">Action</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tickets.map((ticket) => (
            <TableRow key={ticket.id} className="group">
              <TableCell className="font-mono text-xs font-medium text-indigo-600 dark:text-indigo-400">
                <Link href={`${basePath}/${ticket.id}`} className="hover:underline">
                  {ticket.ticketNumber}
                </Link>
              </TableCell>

              <TableCell className="max-w-xs truncate">
                <Link
                  href={`${basePath}/${ticket.id}`}
                  className="font-medium text-slate-900 dark:text-slate-100 hover:text-indigo-600 dark:hover:text-indigo-400 block truncate"
                >
                  {ticket.subject}
                </Link>
              </TableCell>

              {showCustomer && (
                <TableCell className="hidden md:table-cell text-xs text-slate-600 dark:text-slate-300">
                  {ticket.customerName || '—'}
                </TableCell>
              )}

              <TableCell className="hidden sm:table-cell text-xs text-slate-600 dark:text-slate-300">
                {ticket.categoryName ? (
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-[11px] font-medium text-slate-700 dark:text-slate-300">
                    {ticket.categoryName}
                  </span>
                ) : (
                  <span className="text-slate-400 italic text-xs">Uncategorized</span>
                )}
              </TableCell>

              <TableCell>
                <PriorityBadge priority={ticket.priority} />
              </TableCell>

              <TableCell>
                <StatusBadge status={ticket.status} />
              </TableCell>

              <TableCell className="hidden lg:table-cell text-xs text-slate-600 dark:text-slate-300">
                {ticket.assignedTeamName || <span className="text-slate-400 italic">Unassigned</span>}
              </TableCell>

              <TableCell className="hidden lg:table-cell text-xs text-slate-500 dark:text-slate-400">
                {formatDate(ticket.createdAt)}
              </TableCell>

              <TableCell className="text-right">
                <Link
                  href={`${basePath}/${ticket.id}`}
                  className="inline-flex p-1.5 rounded-md text-slate-400 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                  title="View Ticket"
                >
                  <Eye className="w-4 h-4 hidden sm:block" />
                  <ArrowRight className="w-4 h-4 sm:hidden" />
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
