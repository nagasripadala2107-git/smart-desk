import React from 'react';
import { TicketStatus } from '@/types';
import { STATUS_COLORS } from '@/lib/constants';
import { cn } from '@/lib/utils';

interface StatusBadgeProps {
  status: TicketStatus;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = STATUS_COLORS[status] || {
    bg: 'bg-slate-100',
    text: 'text-slate-700',
    border: 'border-slate-300',
    label: status,
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium',
        config.bg,
        config.text,
        config.border,
        className
      )}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-80" />
      {config.label}
    </span>
  );
}
