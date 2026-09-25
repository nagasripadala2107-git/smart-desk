import { TicketPriority, TicketStatus, UserRole } from '@/types';

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  (typeof window !== 'undefined'
    ? (window.location.port === '3000' && window.location.hostname === 'localhost'
        ? `${window.location.protocol}//${window.location.hostname}:8080/api/v1`
        : '/api/v1')
    : (process.env.INTERNAL_BACKEND_URL ? `${process.env.INTERNAL_BACKEND_URL}/api/v1` : 'http://localhost:8080/api/v1'));

export const ROLE_DASHBOARDS: Record<UserRole, string> = {
  CUSTOMER: '/customer/dashboard',
  AGENT: '/agent/dashboard',
  ADMIN: '/admin/dashboard',
};

export const STATUS_COLORS: Record<TicketStatus, { bg: string; text: string; border: string; label: string }> = {
  OPEN: { bg: 'bg-blue-50 dark:bg-blue-950/40', text: 'text-blue-700 dark:text-blue-300', border: 'border-blue-200 dark:border-blue-800', label: 'Open' },
  NEW: { bg: 'bg-blue-50 dark:bg-blue-950/40', text: 'text-blue-700 dark:text-blue-300', border: 'border-blue-200 dark:border-blue-800', label: 'New' },
  ASSIGNED: { bg: 'bg-indigo-50 dark:bg-indigo-950/40', text: 'text-indigo-700 dark:text-indigo-300', border: 'border-indigo-200 dark:border-indigo-800', label: 'Assigned' },
  IN_PROGRESS: { bg: 'bg-amber-50 dark:bg-amber-950/40', text: 'text-amber-700 dark:text-amber-300', border: 'border-amber-200 dark:border-amber-800', label: 'In Progress' },
  PENDING_CUSTOMER: { bg: 'bg-purple-50 dark:bg-purple-950/40', text: 'text-purple-700 dark:text-purple-300', border: 'border-purple-200 dark:border-purple-800', label: 'Pending Customer' },
  PENDING_INTERNAL: { bg: 'bg-indigo-50 dark:bg-indigo-950/40', text: 'text-indigo-700 dark:text-indigo-300', border: 'border-indigo-200 dark:border-indigo-800', label: 'Pending Internal' },
  RESOLVED: { bg: 'bg-emerald-50 dark:bg-emerald-950/40', text: 'text-emerald-700 dark:text-emerald-300', border: 'border-emerald-200 dark:border-emerald-800', label: 'Resolved' },
  CLOSED: { bg: 'bg-slate-100 dark:bg-slate-800', text: 'text-slate-700 dark:text-slate-300', border: 'border-slate-300 dark:border-slate-700', label: 'Closed' },
  ESCALATED: { bg: 'bg-rose-50 dark:bg-rose-950/40', text: 'text-rose-700 dark:text-rose-300', border: 'border-rose-200 dark:border-rose-800', label: 'Escalated' },
};

export const PRIORITY_COLORS: Record<TicketPriority, { bg: string; text: string; border: string; label: string }> = {
  LOW: { bg: 'bg-slate-100 dark:bg-slate-800', text: 'text-slate-700 dark:text-slate-300', border: 'border-slate-200 dark:border-slate-700', label: 'Low' },
  MEDIUM: { bg: 'bg-sky-50 dark:bg-sky-950/40', text: 'text-sky-700 dark:text-sky-300', border: 'border-sky-200 dark:border-sky-800', label: 'Medium' },
  HIGH: { bg: 'bg-amber-50 dark:bg-amber-950/40', text: 'text-amber-700 dark:text-amber-300', border: 'border-amber-200 dark:border-amber-800', label: 'High' },
  URGENT: { bg: 'bg-rose-50 dark:bg-rose-950/40', text: 'text-rose-700 dark:text-rose-300', border: 'border-rose-200 dark:border-rose-800', label: 'Urgent' },
};
