import React from 'react';
import { TeamWorkloadResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Users2 } from 'lucide-react';

interface TeamWorkloadTableProps {
  data: TeamWorkloadResponse[];
}

export function TeamWorkloadTable({ data }: TeamWorkloadTableProps) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center gap-2">
          <Users2 className="w-4 h-4 text-indigo-500" />
          <CardTitle className="text-base font-semibold">Team Workload</CardTitle>
        </div>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Operational ticket distribution across configured teams
        </p>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <div className="py-8 text-center text-xs text-slate-400">
            No team workload data available
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="pb-3 font-medium">Team</th>
                  <th className="pb-3 font-medium text-center">Active Tickets</th>
                  <th className="pb-3 font-medium text-center">Resolved / Closed</th>
                  <th className="pb-3 font-medium text-center">Escalated</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
                {data.map((team) => (
                  <tr key={team.teamId} className="hover:bg-slate-50/50 dark:hover:bg-slate-900/50 transition-colors">
                    <td className="py-3 font-medium text-slate-900 dark:text-slate-100">
                      {team.teamName}
                    </td>
                    <td className="py-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-400">
                        {team.activeTickets}
                      </span>
                    </td>
                    <td className="py-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400">
                        {team.resolvedClosedTickets}
                      </span>
                    </td>
                    <td className="py-3 text-center">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                        team.escalatedTickets > 0
                          ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400'
                          : 'bg-slate-50 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
                      }`}>
                        {team.escalatedTickets}
                      </span>
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
