'use client';

import React, { useEffect, useState, use } from 'react';
import { api } from '@/lib/api';
import { TicketDetailResponse, MessageResponse } from '@/types';
import { useAuth } from '@/hooks/useAuth';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { PriorityBadge } from '@/components/common/PriorityBadge';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { MessageThread } from '@/components/tickets/MessageThread';
import { MessageComposer } from '@/components/tickets/MessageComposer';
import { TicketTimeline } from '@/components/tickets/TicketTimeline';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { formatDate } from '@/lib/utils';
import {
  Tag,
  Headphones,
  Calendar,
  Building,
  XCircle,
  Clock,
  Sparkles,
} from 'lucide-react';

interface PageParams {
  params: Promise<{ id: string }>;
}

export default function CustomerTicketDetailPage({ params }: PageParams) {
  const resolvedParams = use(params);
  const ticketId = resolvedParams.id;
  const { user } = useAuth();

  const [ticket, setTicket] = useState<TicketDetailResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isClosingTicket, setIsClosingTicket] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  const fetchTicketDetails = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [ticketData, messagesData] = await Promise.all([
        api.get<TicketDetailResponse>(`/tickets/${ticketId}`),
        api.get<MessageResponse[]>(`/tickets/${ticketId}/messages`),
      ]);
      setTicket(ticketData);
      setMessages(messagesData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load ticket details');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTicketDetails();
  }, [ticketId]);

  const handleSendMessage = async (messageText: string) => {
    const newMsg = await api.post<MessageResponse>(`/tickets/${ticketId}/messages`, {
      message: messageText,
      isInternal: false,
    });
    setMessages((prev) => [...prev, newMsg]);
  };

  const handleCloseTicket = async () => {
    setIsClosingTicket(true);
    try {
      await api.patch(`/tickets/${ticketId}`, {
        status: 'CLOSED',
        updateReason: 'Closed by customer request',
      });
      setShowCloseConfirm(false);
      await fetchTicketDetails();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to close ticket');
    } finally {
      setIsClosingTicket(false);
    }
  };

  if (isLoading) {
    return <LoadingSkeleton type="details" />;
  }

  if (error || !ticket) {
    return (
      <div className="py-12">
        <ErrorState
          title="Ticket not found or error loading"
          message={error || 'Unable to retrieve ticket'}
          onRetry={fetchTicketDetails}
        />
      </div>
    );
  }

  const isClosedOrResolved = ticket.status === 'CLOSED' || ticket.status === 'RESOLVED';

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <PageHeader
        title={ticket.subject}
        breadcrumbs={[
          { label: 'Dashboard', href: '/customer/dashboard' },
          { label: 'Tickets', href: '/customer/tickets' },
          { label: ticket.ticketNumber },
        ]}
        actions={
          <div className="flex items-center gap-2">
            {!isClosedOrResolved && (
              <Button
                variant="outline"
                size="sm"
                className="text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 gap-1.5"
                onClick={() => setShowCloseConfirm(true)}
              >
                <XCircle className="w-4 h-4" />
                Close Ticket
              </Button>
            )}
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content: Description & Conversation */}
        <div className="lg:col-span-2 space-y-6">
          {/* Issue Description Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
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
              <CardTitle className="text-sm">Conversation & Updates</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <MessageThread messages={messages} currentUserId={user?.id} />

              {!isClosedOrResolved ? (
                <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                  <h4 className="text-xs font-semibold text-slate-900 dark:text-slate-100 mb-2">
                    Reply to Support Team
                  </h4>
                  <MessageComposer onSendMessage={handleSendMessage} allowInternalNotes={false} />
                </div>
              ) : (
                <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 text-center text-xs text-slate-500">
                  This ticket is {ticket.status.toLowerCase()}. Replies are disabled.
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Sidebar Metadata & Timeline */}
        <div className="space-y-6">
          {/* Ticket Information Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Ticket Information</CardTitle>
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
                  Assigned Team
                </span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {ticket.assignedTeamName || <span className="text-slate-400 italic">Unassigned</span>}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Headphones className="w-3.5 h-3.5 text-slate-400" />
                  Assigned Agent
                </span>
                <span className="font-medium text-slate-900 dark:text-slate-100">
                  {ticket.assignedAgentName || <span className="text-slate-400 italic">Triage Queue</span>}
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
                  Last Updated
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

          {/* Audit Timeline Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">Activity & Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              <TicketTimeline events={ticket.events} />
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Confirm Close Modal */}
      <ConfirmDialog
        isOpen={showCloseConfirm}
        onClose={() => setShowCloseConfirm(false)}
        onConfirm={handleCloseTicket}
        title="Close this ticket?"
        message="Are you sure you want to close this ticket? You can submit a new ticket if the issue reoccurs."
        confirmLabel="Close Ticket"
        isDangerous={true}
        isLoading={isClosingTicket}
      />
    </div>
  );
}
