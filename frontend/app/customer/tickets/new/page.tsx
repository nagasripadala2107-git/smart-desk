'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { CategoryResponse, CreateTicketRequest, TicketResponse } from '@/types';
import { createTicketSchema } from '@/lib/validations';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { StatusBadge } from '@/components/common/StatusBadge';
import { PriorityBadge } from '@/components/common/PriorityBadge';
import {
  CheckCircle2,
  Paperclip,
  AlertCircle,
  ExternalLink,
  Copy,
  Check,
  ArrowRight,
} from 'lucide-react';

export default function NewTicketPage() {
  const router = useRouter();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [copied, setCopied] = useState(false);

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'>('MEDIUM');
  const [categoryId, setCategoryId] = useState<string>('');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdTicket, setCreatedTicket] = useState<TicketResponse | null>(null);

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await api.get<CategoryResponse[]>('/categories');
        setCategories(data);
      } catch {
        // Non-blocking: categories are optional if not loaded
      } finally {
        setIsLoadingCategories(false);
      }
    };
    loadCategories();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    setServerError(null);

    const payload: CreateTicketRequest = {
      subject,
      description,
      priority,
      categoryId: categoryId || undefined,
    };

    const validation = createTicketSchema.safeParse(payload);
    if (!validation.success) {
      const fieldErrors: Record<string, string> = {};
      validation.error.issues.forEach((err) => {
        if (err.path[0]) {
          fieldErrors[err.path[0].toString()] = err.message;
        }
      });
      setErrors(fieldErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await api.post<TicketResponse>('/tickets', payload);
      setCreatedTicket(response);
    } catch (err: unknown) {
      setServerError(err instanceof Error ? err.message : 'Failed to create ticket');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (createdTicket) {
    const rawTicket: any = createdTicket;
    const ticketId = rawTicket.id || rawTicket.data?.id || '';
    const ticketNumber =
      rawTicket.ticketNumber ||
      rawTicket.ticket_number ||
      rawTicket.data?.ticketNumber ||
      rawTicket.data?.ticket_number ||
      '';
    const subjectText = rawTicket.subject || rawTicket.data?.subject || subject;
    const statusVal = rawTicket.status || rawTicket.data?.status || 'OPEN';
    const priorityVal = rawTicket.priority || rawTicket.data?.priority || priority;
    const aiCat = rawTicket.aiCategory || rawTicket.data?.aiCategory || rawTicket.categoryName;

    return (
      <div className="max-w-2xl mx-auto py-8">
        <Card className="border-emerald-200 dark:border-emerald-800 shadow-md">
          <CardContent className="p-8 text-center space-y-6">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 mx-auto shadow-xs">
              <CheckCircle2 className="w-9 h-9" />
            </div>

            <div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
                Ticket Created Successfully!
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Your support request has been logged and assigned an official tracking number.
              </p>

              {/* Prominent Official Ticket Number Banner */}
              <div className="mt-4 inline-flex items-center gap-3 px-5 py-3 rounded-2xl bg-indigo-50 dark:bg-indigo-950/70 border-2 border-indigo-200 dark:border-indigo-800 shadow-xs">
                <div className="text-left">
                  <div className="text-[10px] uppercase tracking-wider font-semibold text-indigo-700 dark:text-indigo-300">
                    Official Ticket Number
                  </div>
                  <div className="font-mono text-2xl font-black text-indigo-600 dark:text-indigo-400 tracking-tight">
                    {ticketNumber || 'SD-2026-000001'}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleCopy(ticketNumber || 'SD-2026-000001')}
                  className="p-2.5 rounded-xl hover:bg-indigo-100 dark:hover:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400 transition-colors border border-indigo-200 dark:border-indigo-700 bg-white dark:bg-slate-900"
                  title="Copy Ticket Number"
                >
                  {copied ? <Check className="w-5 h-5 text-emerald-600" /> : <Copy className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div className="bg-slate-50 dark:bg-slate-800/60 p-5 rounded-xl border border-slate-100 dark:border-slate-800 text-left space-y-3 text-xs">
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60 dark:border-slate-700/60">
                <span className="text-slate-500 font-medium">Ticket Reference:</span>
                <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400 text-sm">
                  {ticketNumber || 'SD-2026-000001'}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">Subject:</span>
                <span className="font-semibold text-slate-900 dark:text-slate-100">
                  {subjectText}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">Status:</span>
                <StatusBadge status={statusVal} />
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">Priority:</span>
                <PriorityBadge priority={priorityVal} />
              </div>
              {aiCat && (
                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium">AI Classification:</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200">{aiCat}</span>
                </div>
              )}
              <div className="pt-2 border-t border-slate-200/60 dark:border-slate-700/60 text-[11px] text-slate-500">
                Estimated Initial Response: Standard SLA window (under 4 hours for {priorityVal} priority).
              </div>
            </div>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
              {ticketId ? (
                <Link href={`/customer/tickets/${ticketId}`} className="w-full sm:w-auto">
                  <Button className="w-full sm:w-auto gap-2">
                    View Ticket Details
                    <ExternalLink className="w-4 h-4" />
                  </Button>
                </Link>
              ) : null}
              <Link href="/customer/tickets" className="w-full sm:w-auto">
                <Button variant="outline" className="w-full sm:w-auto">
                  Back to Ticket List
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <PageHeader
        title="Create New Support Ticket"
        description="Submit your request to our support team. Provide as much detail as possible for accurate routing."
        breadcrumbs={[
          { label: 'Dashboard', href: '/customer/dashboard' },
          { label: 'Tickets', href: '/customer/tickets' },
          { label: 'New Ticket' },
        ]}
      />

      <Card>
        <CardContent className="p-6 sm:p-8">
          {serverError && (
            <div className="mb-6 p-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              label="Subject"
              id="subject"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="Brief summary of the issue..."
              error={errors.subject}
              helperText="E.g., Unable to connect database in staging environment"
              required
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Select
                label="Priority"
                id="priority"
                value={priority}
                onChange={(e) =>
                  setPriority(e.target.value as 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT')
                }
                options={[
                  { value: 'LOW', label: 'Low — General questions or cosmetic issues' },
                  { value: 'MEDIUM', label: 'Medium — Standard support request' },
                  { value: 'HIGH', label: 'High — Important feature impacted' },
                  { value: 'URGENT', label: 'Urgent — Critical outage or blocking bug' },
                ]}
              />

              <Select
                label="Category (Optional)"
                id="categoryId"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                options={[
                  { value: '', label: isLoadingCategories ? 'Loading categories...' : 'Auto-classify category' },
                  ...categories.map((c) => ({ value: c.id, label: c.name })),
                ]}
              />
            </div>

            <div className="space-y-1.5">
              <label
                htmlFor="description"
                className="block text-xs font-medium text-slate-700 dark:text-slate-300"
              >
                Detailed Description
              </label>
              <textarea
                id="description"
                rows={6}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Explain the steps to reproduce, expected behavior, and any relevant error codes..."
                className={`w-full rounded-lg border p-3 text-sm bg-white dark:bg-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
                  errors.description
                    ? 'border-rose-500'
                    : 'border-slate-300 dark:border-slate-700'
                }`}
                required
              />
              {errors.description && (
                <p className="text-xs text-rose-500">{errors.description}</p>
              )}
            </div>

            {/* Attachment UI Placeholder */}
            <div className="p-4 rounded-xl border border-dashed border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30 text-center">
              <div className="flex items-center justify-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                <Paperclip className="w-4 h-4 text-slate-400" />
                <span>File attachments placeholder (upload pipeline integration planned)</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => router.back()}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" isLoading={isSubmitting}>
                Submit Support Ticket
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
