import React from 'react';
import { TicketPriority } from '@/types';
import { PRIORITY_COLORS } from '@/lib/constants';
import { cn } from '@/lib/utils';
import { AlertCircle, AlertTriangle, ArrowDown, ArrowUp } from 'lucide-react';

interface PriorityBadgeProps {
  priority: TicketPriority;
  className?: string;
}

export function PriorityBadge({ priority, className }: PriorityBadgeProps) {
  const config = PRIORITY_COLORS[priority] || {
    bg: 'bg-slate-100',
    text: 'text-slate-700',
    border: 'border-slate-300',
    label: priority,
  };

  const renderIcon = () => {
    switch (priority) {
      case 'URGENT':
        return <AlertCircle className="w-3 h-3 text-rose-600" />;
      case 'HIGH':
        return <AlertTriangle className="w-3 h-3 text-amber-600" />;
      case 'MEDIUM':
        return <ArrowUp className="w-3 h-3 text-sky-600" />;
      case 'LOW':
        return <ArrowDown className="w-3 h-3 text-slate-500" />;
      default:
        return null;
    }
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-xs font-medium',
        config.bg,
        config.text,
        config.border,
        className
      )}
    >
      {renderIcon()}
      {config.label}
    </span>
  );
}
