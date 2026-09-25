import React from 'react';
import { EscalationStatsResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { AlertOctagon, ArrowUpRight } from 'lucide-react';

interface EscalationOverviewCardProps {
  data: EscalationStatsResponse;
}

export function EscalationOverviewCard({ data }: EscalationOverviewCardProps) {
  const levelEntries = Object.entries(data.byLevel || {});
  const teamEntries = Object.entries(data.byTeam || {});

  return (
    <Card className="flex flex-col">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertOctagon className="w-4 h-4 text-rose-500" />
            <CardTitle className="text-base font-semibold">Escalation Overview</CardTitle>
          </div>
          <span className="text-xs px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400 font-semibold">
            {data.totalEscalations} Total
          </span>
        </div>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Escalation tiers, target teams, and recent events
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Tier & Target Team Breakdown */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800">
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-2">
              By Escalation Level
            </p>
            {levelEntries.length === 0 ? (
              <p className="text-xs text-slate-400">No tier escalations</p>
            ) : (
              <div className="space-y-1.5">
                {levelEntries.map(([lvl, count]) => (
                  <div key={lvl} className="flex items-center justify-between text-xs">
                    <span className="text-slate-600 dark:text-slate-300">Level {lvl}</span>
                    <span className="font-semibold text-slate-900 dark:text-slate-100">{count}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800">
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-2">
              By Target Team
            </p>
            {teamEntries.length === 0 ? (
              <p className="text-xs text-slate-400">No team escalations</p>
            ) : (
              <div className="space-y-1.5">
                {teamEntries.map(([team, count]) => (
                  <div key={team} className="flex items-center justify-between text-xs">
                    <span className="text-slate-600 dark:text-slate-300 truncate max-w-[120px]">
                      {team}
                    </span>
                    <span className="font-semibold text-slate-900 dark:text-slate-100">{count}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Recent Escalation Activity */}
        <div>
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-2">
            Recent Escalations
          </p>
          {data.recentEscalations.length === 0 ? (
            <p className="text-xs text-slate-400 py-2">No recent escalation activity</p>
          ) : (
            <div className="space-y-2">
              {data.recentEscalations.slice(0, 4).map((esc) => (
                <div
                  key={esc.id}
                  className="flex items-start justify-between p-2.5 rounded-lg border border-slate-100 dark:border-slate-800 text-xs hover:bg-slate-50/50 dark:hover:bg-slate-900/50 transition-colors"
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono font-medium text-indigo-600 dark:text-indigo-400">
                        {esc.ticketNumber}
                      </span>
                      <span className="px-1.5 py-0.2 rounded text-[10px] bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400">
                        L{esc.level}
                      </span>
                    </div>
                    <p className="text-slate-600 dark:text-slate-300 text-[11px] line-clamp-1">
                      {esc.reason}
                    </p>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 flex items-center gap-0.5 justify-end">
                      <ArrowUpRight className="w-3 h-3" />
                      {esc.toTeamName}
                    </span>
                    <span className="text-[10px] text-slate-400">
                      {new Date(esc.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
