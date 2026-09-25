'use client';

import React, { useEffect, useState, use } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import {
  TicketDetailResponse,
  MessageResponse,
  TeamResponse,
  CategoryResponse,
  TicketStatus,
  TicketPriority,
} from '@/types';
import { useAuth } from '@/hooks/useAuth';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { PriorityBadge } from '@/components/common/PriorityBadge';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { Modal } from '@/components/ui/Modal';
import { MessageThread } from '@/components/tickets/MessageThread';
import { MessageComposer } from '@/components/tickets/MessageComposer';
import { TicketTimeline } from '@/components/tickets/TicketTimeline';
import { CustomerSentimentCard } from '@/components/tickets/CustomerSentimentCard';
import { PotentialDuplicatesCard } from '@/components/tickets/PotentialDuplicatesCard';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { formatDate } from '@/lib/utils';
import {
  AlertOctagon,
  CheckCircle,
  Building,
  User,
  Tag,
  Calendar,
  Clock,
  ExternalLink,
  Sparkles,
} from 'lucide-react';

interface PageParams {
  params: Promise<{ id: string }>;
}

export default function AgentTicketDetailPage({ params }: PageParams) {
  const resolvedParams = use(params);
  const ticketId = resolvedParams.id;
  const { user } = useAuth();

  const [ticket, setTicket] = useState<TicketDetailResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [teams, setTeams] = useState<TeamResponse[]>([]);
  const [_categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Status & Priority quick changes
  const [status, setStatus] = useState<TicketStatus>('NEW');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Escalation Modal state
  const [isEscalateModalOpen, setIsEscalateModalOpen] = useState(false);
  const [targetTeamId, setTargetTeamId] = useState('');
  const [escalationLevel, setEscalationLevel] = useState(2);
  const [escalationReason, setEscalationReason] = useState('');
  const [isEscalating, setIsEscalating] = useState(false);
  const [escalationError, setEscalationError] = useState<string | null>(null);

  const fetchTicketDetails = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [ticketData, messagesData, teamsData, categoriesData] = await Promise.all([
        api.get<TicketDetailResponse>(`/tickets/${ticketId}`),
        api.get<MessageResponse[]>(`/tickets/${ticketId}/messages`),
        api.get<TeamResponse[]>('/teams').catch(() => []),
        api.get<CategoryResponse[]>('/categories').catch(() => []),
      ]);
      setTicket(ticketData);
      setMessages(messagesData);
      setTeams(teamsData);
      setCategories(categoriesData);
      setStatus(ticketData.status);
      setPriority(ticketData.priority);
      if (teamsData.length > 0) {
        setTargetTeamId(teamsData[0].id);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load ticket details');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTicketDetails();
  }, [ticketId]);

  const handleSendMessage = async (messageText: string, isInternal: boolean) => {
    const newMsg = await api.post<MessageResponse>(`/tickets/${ticketId}/messages`, {
      message: messageText,
      isInternal,
    });
    setMessages((prev) => [...prev, newMsg]);
  };

  const handleUpdateTicket = async (newStatus: TicketStatus, newPriority: TicketPriority) => {
    setIsUpdatingStatus(true);
    try {
      await api.patch(`/tickets/${ticketId}`, {
        status: newStatus,
        priority: newPriority,
        updateReason: `Status updated to ${newStatus} by agent`,
      });
      setStatus(newStatus);
      setPriority(newPriority);
      await fetchTicketDetails();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to update ticket');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleEscalateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetTeamId || !escalationReason.trim()) return;

    setIsEscalating(true);
    setEscalationError(null);
    try {
      await api.post(`/tickets/${ticketId}/escalate`, {
        targetTeamId,
        level: Number(escalationLevel),
        reason: escalationReason.trim(),
      });
      setIsEscalateModalOpen(false);
      setEscalationReason('');
      await fetchTicketDetails();
    } catch (err: unknown) {
      setEscalationError(err instanceof Error ? err.message : 'Failed to escalate ticket');
    } finally {
      setIsEscalating(false);
    }
  };

  if (isLoading) {
    return <LoadingSkeleton type="details" />;
  }

  if (error || !ticket) {
    return (
      <div className="py-12">
        <ErrorState
          title="Ticket Unavailable"
          message={error || 'Failed to load ticket details'}
          onRetry={fetchTicketDetails}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <PageHeader
        title={ticket.subject}
        breadcrumbs={[
          { label: 'Dashboard', href: '/agent/dashboard' },
          { label: 'My Tickets', href: '/agent/tickets' },
          { label: ticket.ticketNumber },
        ]}
        actions={
          <div className="flex items-center gap-2">
            {ticket.status !== 'RESOLVED' && (
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/30"
                onClick={() => handleUpdateTicket('RESOLVED', priority)}
                disabled={isUpdatingStatus}
              >
                <CheckCircle className="w-4 h-4" />
                Resolve Ticket
              </Button>
            )}

            <Button
              variant="outline"
              size="sm"
              className="gap-1.5 text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30"
              onClick={() => setIsEscalateModalOpen(true)}
            >
              <AlertOctagon className="w-4 h-4" />
              Escalate
            </Button>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Issue & Conversation */}
        <div className="lg:col-span-2 space-y-6">
          {/* Main Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <span className="font-mono text-xs font-semibold text-indigo-600 dark:text-indigo-400">
                  {ticket.ticketNumber}
                </span>
                <CardTitle className="text-base mt-1">{ticket.subject}</CardTitle>
              </div>
              <div className="flex items-center gap-2">
                <PriorityBadge priority={ticket.priority} />
                <StatusBadge status={ticket.status} />
              </div>
            </CardHeader>
            <CardContent>
              <div className="prose prose-sm dark:prose-invert max-w-none text-xs sm:text-sm text-slate-700 dark:text-slate-300 whitespace-pre-wrap leading-relaxed">
                {ticket.description}
              </div>
            </CardContent>
          </Card>

          {/* Conversation Thread */}
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Conversation & Internal Notes</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <MessageThread messages={messages} currentUserId={user?.id} />

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                <h4 className="text-xs font-semibold text-slate-900 dark:text-slate-100 mb-2">
                  Post Update / Reply
                </h4>
                <MessageComposer onSendMessage={handleSendMessage} allowInternalNotes={true} />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Right Col: Ticket Controls & Metadata */}
        <div className="space-y-6">
          {/* Quick Controls Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Ticket Controls</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-medium text-slate-600 dark:text-slate-400">
                  Status
                </label>
                <Select
                  value={status}
                  onChange={(e) => {
                    const newStatus = e.target.value as TicketStatus;
                    setStatus(newStatus);
                    handleUpdateTicket(newStatus, priority);
                  }}
                  disabled={isUpdatingStatus}
                  options={[
                    { value: 'NEW', label: 'New' },
                    { value: 'ASSIGNED', label: 'Assigned' },
                    { value: 'IN_PROGRESS', label: 'In Progress' },
                    { value: 'PENDING_CUSTOMER', label: 'Pending Customer' },
                    { value: 'RESOLVED', label: 'Resolved' },
                    { value: 'CLOSED', label: 'Closed' },
                    { value: 'ESCALATED', label: 'Escalated' },
                  ]}
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-medium text-slate-600 dark:text-slate-400">
                  Priority
                </label>
                <Select
                  value={priority}
                  onChange={(e) => {
                    const newPriority = e.target.value as TicketPriority;
                    setPriority(newPriority);
                    handleUpdateTicket(status, newPriority);
                  }}
                  disabled={isUpdatingStatus}
                  options={[
                    { value: 'LOW', label: 'Low' },
                    { value: 'MEDIUM', label: 'Medium' },
                    { value: 'HIGH', label: 'High' },
                    { value: 'URGENT', label: 'Urgent' },
                  ]}
                />
              </div>
            </CardContent>
          </Card>

          {/* Customer 360 Link Card */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm">Customer Context</CardTitle>
                <Link
                  href={`/agent/customers/${ticket.customerId}`}
                  className="text-xs font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 flex items-center gap-1"
                >
                  360° View
                  <ExternalLink className="w-3.5 h-3.5" />
                </Link>
              </div>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Name:</span>
                <span className="font-semibold text-slate-900 dark:text-slate-100">
                  {ticket.customerName}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Organization:</span>
                <span className="font-medium text-slate-800 dark:text-slate-200">
                  {ticket.companyName || 'Personal'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Code:</span>
                <span className="font-mono text-indigo-600 dark:text-indigo-400">
                  {ticket.customerCode || '—'}
                </span>
              </div>
            </CardContent>
          </Card>

          {/* Ticket Information */}
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Metadata & Assignment</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Tag className="w-3.5 h-3.5 text-slate-400" />
                  Category
                </span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {ticket.categoryName || 'General Support'}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Building className="w-3.5 h-3.5 text-slate-400" />
                  Team
                </span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {ticket.assignedTeamName || 'Unassigned'}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5 text-slate-400" />
                  Assigned Agent
                </span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {ticket.assignedAgentName || 'Unassigned'}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  Created
                </span>
                <span className="text-slate-700 dark:text-slate-300">{formatDate(ticket.createdAt)}</span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  Updated
                </span>
                <span className="text-slate-700 dark:text-slate-300">{formatDate(ticket.updatedAt)}</span>
              </div>

              {ticket.aiCategory && (
                <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-indigo-500" />
                    AI Classification
                  </span>
                  <span className="font-medium text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
                    {ticket.aiCategory}
                    {ticket.aiConfidence != null && (
                      <span className="text-[11px] text-slate-500 font-mono">
                        ({(ticket.aiConfidence * 100).toFixed(1)}%)
                      </span>
                    )}
                  </span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Customer Sentiment Analysis (Phase 8.2) */}
          <CustomerSentimentCard
            sentiment={ticket.sentiment}
            confidence={ticket.sentimentConfidence}
            tone={ticket.tone}
            modelVersion={ticket.sentimentModelVersion}
          />

          {/* Potential Duplicate Tickets (Phase 8.3) */}
          <PotentialDuplicatesCard
            matches={ticket.duplicateMatches}
          />

          {/* Audit Timeline */}
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Audit History</CardTitle>
            </CardHeader>
            <CardContent>
              <TicketTimeline events={ticket.events} />
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Escalation Modal */}
      <Modal
        isOpen={isEscalateModalOpen}
        onClose={() => setIsEscalateModalOpen(false)}
        title="Escalate Support Ticket"
        description="Transfer ticket ownership to a specialized tier or team with audit tracking."
      >
        {escalationError && (
          <div className="mb-4 p-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-xs">
            {escalationError}
          </div>
        )}

        <form onSubmit={handleEscalateSubmit} className="space-y-4">
          <Select
            label="Target Escalation Team"
            value={targetTeamId}
            onChange={(e) => setTargetTeamId(e.target.value)}
            options={teams.map((t) => ({ value: t.id, label: `${t.name} (${t.agentCount} agents)` }))}
            required
          />

          <Select
            label="Escalation Tier Level"
            value={String(escalationLevel)}
            onChange={(e) => setEscalationLevel(Number(e.target.value))}
            options={[
              { value: '2', label: 'Tier 2 — Senior Engineering / Technical Investigation' },
              { value: '3', label: 'Tier 3 — Core Infrastructure / Engineering Leads' },
              { value: '4', label: 'Tier 4 — Executive / Critical Incident Commander' },
            ]}
          />

          <div className="space-y-1">
            <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Escalation Reason
            </label>
            <textarea
              rows={3}
              value={escalationReason}
              onChange={(e) => setEscalationReason(e.target.value)}
              placeholder="Detail why standard resolution was blocked and what assistance is needed..."
              className="w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 p-2.5 text-xs sm:text-sm placeholder:text-slate-400 focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsEscalateModalOpen(false)}
              disabled={isEscalating}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="danger"
              size="sm"
              isLoading={isEscalating}
              disabled={!targetTeamId || !escalationReason.trim()}
            >
              Confirm Escalation
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
