import React from 'react';
import { TicketEventDto } from '@/types';
import { formatDate } from '@/lib/utils';
import {
  CheckCircle,
  Clock,
  UserCheck,
  AlertTriangle,
  ArrowRight,
  MessageSquare,
  FileText,
} from 'lucide-react';

interface TicketTimelineProps {
  events: TicketEventDto[];
}

export function TicketTimeline({ events }: TicketTimelineProps) {
  if (!events || events.length === 0) {
    return (
      <div className="py-6 text-center text-xs text-slate-500 dark:text-slate-400">
        No audit events recorded yet for this ticket.
      </div>
    );
  }

  const getEventIcon = (type: string) => {
    switch (type) {
      case 'CREATED':
        return <FileText className="w-4 h-4 text-blue-500" />;
      case 'STATUS_CHANGED':
        return <Clock className="w-4 h-4 text-amber-500" />;
      case 'ASSIGNED':
      case 'REASSIGNED':
        return <UserCheck className="w-4 h-4 text-indigo-500" />;
      case 'ESCALATED':
        return <AlertTriangle className="w-4 h-4 text-rose-500" />;
      case 'RESOLVED':
      case 'CLOSED':
        return <CheckCircle className="w-4 h-4 text-emerald-500" />;
      case 'NOTE_ADDED':
        return <MessageSquare className="w-4 h-4 text-slate-500" />;
      default:
        return <Clock className="w-4 h-4 text-slate-400" />;
    }
  };

  const getEventDescription = (event: TicketEventDto) => {
    switch (event.eventType) {
      case 'CREATED':
        return 'Ticket created';
      case 'STATUS_CHANGED':
        return (
          <span className="flex items-center gap-1">
            Status updated from <span className="font-semibold">{event.oldValue || 'None'}</span>
            <ArrowRight className="w-3 h-3 text-slate-400 inline" />
            <span className="font-semibold">{event.newValue}</span>
          </span>
        );
      case 'PRIORITY_CHANGED':
        return (
          <span className="flex items-center gap-1">
            Priority changed from <span className="font-semibold">{event.oldValue || 'None'}</span>
            <ArrowRight className="w-3 h-3 text-slate-400 inline" />
            <span className="font-semibold">{event.newValue}</span>
          </span>
        );
      case 'ASSIGNED':
      case 'REASSIGNED':
        return (
          <span>
            Assigned to <span className="font-semibold">{event.newValue}</span>
          </span>
        );
      case 'ESCALATED':
        return (
          <span className="text-rose-600 dark:text-rose-400">
            Escalated: {event.newValue || 'Level increased'}
          </span>
        );
      case 'RESOLVED':
        return <span className="text-emerald-600 dark:text-emerald-400 font-medium">Ticket marked as resolved</span>;
      case 'CLOSED':
        return 'Ticket closed';
      case 'NOTE_ADDED':
        return 'Internal note added';
      default:
        return event.metadata || event.eventType;
    }
  };

  return (
    <div className="flow-root">
      <ul className="-mb-8">
        {events.map((event, eventIdx) => (
          <li key={event.id || eventIdx}>
            <div className="relative pb-8">
              {eventIdx !== events.length - 1 ? (
                <span
                  className="absolute left-4 top-4 -ml-px h-full w-0.5 bg-slate-200 dark:bg-slate-800"
                  aria-hidden="true"
                />
              ) : null}
              <div className="relative flex space-x-3">
                <div>
                  <span className="h-8 w-8 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center ring-4 ring-white dark:ring-slate-900">
                    {getEventIcon(event.eventType)}
                  </span>
                </div>
                <div className="flex min-w-0 flex-1 justify-between space-x-4 pt-1.5">
                  <div>
                    <div className="text-xs text-slate-800 dark:text-slate-200">
                      {getEventDescription(event)}
                    </div>
                    {event.actorName && (
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                        By <span className="font-medium text-slate-700 dark:text-slate-300">{event.actorName}</span>
                      </p>
                    )}
                  </div>
                  <div className="whitespace-nowrap text-right text-[11px] text-slate-400">
                    <time dateTime={event.createdAt}>{formatDate(event.createdAt)}</time>
                  </div>
                </div>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
