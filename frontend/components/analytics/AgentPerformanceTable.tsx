import React from 'react';
import { AgentPerformanceResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { UserCheck } from 'lucide-react';

interface AgentPerformanceTableProps {
  data: AgentPerformanceResponse[];
}

export function AgentPerformanceTable({ data }: AgentPerformanceTableProps) {
  const formatDuration = (mins?: number | null) => {
    if (mins == null) return '—';
    if (mins < 60) return `${Math.round(mins)} min`;
    const hours = (mins / 60).toFixed(1);
    return `${hours} hrs`;
  };

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center gap-2">
          <UserCheck className="w-4 h-4 text-indigo-500" />
          <CardTitle className="text-base font-semibold">Agent Operational Metrics</CardTitle>
        </div>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Factual ticket handling metrics per agent (Operational data from PostgreSQL)
        </p>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <div className="py-8 text-center text-xs text-slate-400">
            No agent performance data available
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="pb-3 font-medium">Agent</th>
                  <th className="pb-3 font-medium">Code</th>
                  <th className="pb-3 font-medium">Team</th>
                  <th className="pb-3 font-medium text-center">Assigned</th>
                  <th className="pb-3 font-medium text-center">Open</th>
                  <th className="pb-3 font-medium text-center">Resolved</th>
                  <th className="pb-3 font-medium text-right">Avg Resolution Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
                {data.map((agent) => (
                  <tr key={agent.agentId} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/50 transition-colors">
                    <td className="py-3 font-medium text-slate-900 dark:text-slate-100">
                      {agent.agentName}
                    </td>
                    <td className="py-3 text-xs font-mono text-slate-500 dark:text-slate-400">
                      {agent.employeeCode}
                    </td>
                    <td className="py-3 text-xs text-slate-600 dark:text-slate-400">
                      {agent.teamName}
                    </td>
                    <td className="py-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                        {agent.assignedTickets}
                      </span>
                    </td>
                    <td className="py-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 dark:bg-amber-950/60 dark:text-amber-400">
                        {agent.openTickets}
                      </span>
                    </td>
                    <td className="py-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400">
                        {agent.resolvedTickets}
                      </span>
                    </td>
                    <td className="py-3 text-right text-xs font-medium text-slate-700 dark:text-slate-300">
                      {formatDuration(agent.avgResolutionMinutes)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
