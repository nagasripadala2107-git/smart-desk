import React from 'react';
import { AgentResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Headphones, Mail, Hash, Users } from 'lucide-react';

interface AgentSummaryProps {
  agent: AgentResponse;
}

export function AgentSummary({ agent }: AgentSummaryProps) {
  const getAvailabilityBadgeVariant = (status: string) => {
    switch (status) {
      case 'AVAILABLE':
        return 'success';
      case 'BUSY':
        return 'warning';
      case 'AWAY':
      case 'OFFLINE':
      default:
        return 'default';
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm">Assigned Agent</CardTitle>
          <Badge variant={getAvailabilityBadgeVariant(agent.availabilityStatus)}>
            {agent.availabilityStatus}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 text-xs">
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Headphones className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="font-medium text-slate-900 dark:text-slate-100">
            {agent.firstName} {agent.lastName}
          </span>
        </div>
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Mail className="w-4 h-4 text-slate-400 shrink-0" />
          <span>{agent.email}</span>
        </div>
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Users className="w-4 h-4 text-slate-400 shrink-0" />
          <span>Team: {agent.teamName || 'Unassigned'}</span>
        </div>
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Hash className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="font-mono">{agent.employeeCode}</span>
        </div>
        <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex justify-between text-slate-500">
          <span>Active Load: <strong className="text-slate-900 dark:text-slate-100">{agent.currentActiveTickets} / {agent.maxActiveTickets}</strong></span>
        </div>
      </CardContent>
    </Card>
  );
}
