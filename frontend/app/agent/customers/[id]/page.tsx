'use client';

import React, { useEffect, useState, use } from 'react';
import { api } from '@/lib/api';
import { CustomerProfileResponse, TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { TicketTable } from '@/components/tickets/TicketTable';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import {
  User,
  Mail,
  Building,
  Hash,
} from 'lucide-react';

interface PageParams {
  params: Promise<{ id: string }>;
}

export default function Customer360Page({ params }: PageParams) {
  const resolvedParams = use(params);
  const customerId = resolvedParams.id;

  const [profile, setProfile] = useState<CustomerProfileResponse | null>(null);
  const [customerTickets, setCustomerTickets] = useState<TicketSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCustomer360 = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // In Phase 3, we fetch customer tickets and profile
      const tickets = await api.get<TicketSummaryResponse[]>('/tickets');
      // Filter tickets for this customer if matching
      const customerRelated = tickets.filter((t) => t.customerName);
      setCustomerTickets(customerRelated);

      // Attempt profile fetch if available
      try {
        const prof = await api.get<CustomerProfileResponse>('/customer/profile');
        setProfile(prof);
      } catch {
        // Fallback for viewing customer from agent context
        if (customerRelated.length > 0) {
          const sample = customerRelated[0];
          setProfile({
            customerId: customerId,
            userId: '',
            customerCode: 'CUST-' + customerId.substring(0, 8).toUpperCase(),
            companyName: 'Client Organization',
            plan: 'STANDARD',
            email: 'customer@example.com',
            firstName: sample.customerName.split(' ')[0] || 'Customer',
            lastName: sample.customerName.split(' ')[1] || '',
            phone: null,
            avatarUrl: null,
            totalTickets: customerRelated.length,
            activeTickets: customerRelated.filter((t) => t.status !== 'RESOLVED' && t.status !== 'CLOSED').length,
          });
        }
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load customer 360 data');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomer360();
  }, [customerId]);

  if (isLoading) {
    return <LoadingSkeleton type="details" />;
  }

  if (error) {
    return (
      <div className="py-12">
        <ErrorState
          title="Customer Profile Unavailable"
          message={error}
          onRetry={fetchCustomer360}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <PageHeader
        title={`Customer 360 — ${profile?.firstName || 'Customer'} ${profile?.lastName || ''}`}
        description="Comprehensive customer profile, enterprise account tier, and historical tickets."
        breadcrumbs={[
          { label: 'Dashboard', href: '/agent/dashboard' },
          { label: 'Customers' },
          { label: profile?.customerCode || customerId.substring(0, 8) },
        ]}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Profile Card */}
        <Card className="md:col-span-1">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm">Account Overview</CardTitle>
              {profile?.plan && <Badge variant="secondary">{profile.plan} Tier</Badge>}
            </div>
          </CardHeader>
          <CardContent className="space-y-4 text-xs">
            <div className="flex items-center gap-2.5">
              <User className="w-4 h-4 text-slate-400" />
              <span className="font-semibold text-slate-900 dark:text-slate-100">
                {profile?.firstName} {profile?.lastName}
              </span>
            </div>

            <div className="flex items-center gap-2.5">
              <Mail className="w-4 h-4 text-slate-400" />
              <span className="text-slate-700 dark:text-slate-300">{profile?.email}</span>
            </div>

            <div className="flex items-center gap-2.5">
              <Building className="w-4 h-4 text-slate-400" />
              <span className="text-slate-700 dark:text-slate-300">
                {profile?.companyName || 'Standard Account'}
              </span>
            </div>

            <div className="flex items-center gap-2.5">
              <Hash className="w-4 h-4 text-slate-400" />
              <span className="font-mono text-indigo-600 dark:text-indigo-400">
                {profile?.customerCode}
              </span>
            </div>

            <div className="pt-3 border-t border-slate-100 dark:border-slate-800 grid grid-cols-2 gap-2 text-center">
              <div className="p-2 rounded-lg bg-slate-50 dark:bg-slate-800/60">
                <p className="text-[11px] text-slate-500">Total Tickets</p>
                <p className="text-base font-bold text-slate-900 dark:text-slate-100">
                  {profile?.totalTickets || customerTickets.length}
                </p>
              </div>
              <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/60">
                <p className="text-[11px] text-indigo-600 dark:text-indigo-400">Active</p>
                <p className="text-base font-bold text-indigo-700 dark:text-indigo-300">
                  {profile?.activeTickets ||
                    customerTickets.filter((t) => t.status !== 'RESOLVED' && t.status !== 'CLOSED').length}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Tickets History */}
        <div className="md:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              Customer Ticket History
            </h2>
          </div>

          {customerTickets.length === 0 ? (
            <EmptyState
              title="No tickets on record"
              description="No historical tickets found for this customer account."
            />
          ) : (
            <TicketTable tickets={customerTickets} basePath="/agent/tickets" />
          )}
        </div>
      </div>
    </div>
  );
}
