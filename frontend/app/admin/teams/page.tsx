'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { TeamResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { Users, Layers } from 'lucide-react';

export default function AdminTeamsPage() {
  const [teams, setTeams] = useState<TeamResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTeams = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.get<TeamResponse[]>('/teams');
      setTeams(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load teams');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTeams();
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Support Teams"
        description="Routing destination groups and agent workforce pools."
      />

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <LoadingSkeleton rows={3} />
          <LoadingSkeleton rows={3} />
          <LoadingSkeleton rows={3} />
        </div>
      ) : error ? (
        <ErrorState message={error} onRetry={fetchTeams} />
      ) : teams.length === 0 ? (
        <EmptyState
          title="No support teams found"
          description="No active support teams are registered in the backend."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {teams.map((team) => (
            <Card key={team.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
                      <Layers className="w-4 h-4" />
                    </div>
                    <CardTitle className="text-sm font-semibold">{team.name}</CardTitle>
                  </div>
                  <Badge variant={team.isActive ? 'success' : 'default'}>
                    {team.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 text-xs">
                <p className="text-slate-600 dark:text-slate-400 min-h-[36px] leading-relaxed">
                  {team.description || 'Dedicated support operations and issue triage unit.'}
                </p>

                <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-slate-500">
                  <span className="flex items-center gap-1.5">
                    <Users className="w-3.5 h-3.5 text-slate-400" />
                    Agent Workforce
                  </span>
                  <span className="font-semibold text-slate-900 dark:text-slate-100">
                    {team.agentCount} {team.agentCount === 1 ? 'agent' : 'agents'}
                  </span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
