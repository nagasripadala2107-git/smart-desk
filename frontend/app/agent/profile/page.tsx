'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { AgentResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { AgentSummary } from '@/components/dashboard/AgentSummary';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Headphones, Mail, Hash, Users, Award } from 'lucide-react';

export default function AgentProfilePage() {
  const [agent, setAgent] = useState<AgentResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProfile = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.get<AgentResponse>('/agent/profile');
      setAgent(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load agent profile');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <PageHeader
        title="Agent Profile & Workload"
        description="View your assigned support team, active concurrency limits, and availability."
      />

      {isLoading ? (
        <LoadingSkeleton type="details" />
      ) : error || !agent ? (
        <ErrorState
          title="Profile unavailable"
          message={error || 'Failed to retrieve agent data'}
          onRetry={fetchProfile}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Support Credentials</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4 text-sm">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Full Name</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <Headphones className="w-4 h-4 text-slate-400" />
                      {agent.firstName} {agent.lastName}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Work Email</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <Mail className="w-4 h-4 text-slate-400" />
                      {agent.email}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Assigned Team</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <Users className="w-4 h-4 text-slate-400" />
                      {agent.teamName || 'Unassigned'}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Employee Code</span>
                    <p className="font-mono font-bold text-indigo-600 dark:text-indigo-400 flex items-center gap-2">
                      <Hash className="w-4 h-4 text-slate-400" />
                      {agent.employeeCode}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Specialized Skills</span>
                    <p className="font-medium text-slate-800 dark:text-slate-200 flex items-center gap-2">
                      <Award className="w-4 h-4 text-slate-400" />
                      {agent.skills || 'General Support'}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Max Active Concurrency</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100">
                      {agent.maxActiveTickets} Concurrent Tickets
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <AgentSummary agent={agent} />
          </div>
        </div>
      )}
    </div>
  );
}
