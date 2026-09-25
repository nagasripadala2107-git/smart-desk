import React from 'react';
import { SlaAnalyticsResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { ShieldCheck, AlertCircle, Clock } from 'lucide-react';

interface SlaComplianceCardProps {
  data: SlaAnalyticsResponse;
}

export function SlaComplianceCard({ data }: SlaComplianceCardProps) {
  const formatMins = (mins: number) => {
    if (mins < 60) return `${mins}m`;
    return `${(mins / 60).toFixed(0)}h`;
  };

  return (
    <Card className="flex flex-col">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-indigo-500" />
            <CardTitle className="text-base font-semibold">SLA Policy Coverage</CardTitle>
          </div>
          <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400 font-semibold">
            {data.configuredPoliciesCount} Policies Configured
          </span>
        </div>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Measurable thresholds based on active SLA policies (Operational data from PostgreSQL)
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Measurable Threshold Flags */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2 mb-1">
              <Clock className="w-4 h-4 text-amber-500" />
              <p className="text-xs font-medium text-slate-700 dark:text-slate-300">
                Past Response Target
              </p>
            </div>
            <div className="text-2xl font-bold text-slate-900 dark:text-slate-100">
              {data.ticketsPastResponseTargetCount}
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Open tickets exceeding SLA first response threshold
            </p>
          </div>

          <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2 mb-1">
              <AlertCircle className="w-4 h-4 text-rose-500" />
              <p className="text-xs font-medium text-slate-700 dark:text-slate-300">
                Past Resolution Target
              </p>
            </div>
            <div className="text-2xl font-bold text-slate-900 dark:text-slate-100">
              {data.ticketsPastResolutionTargetCount}
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Active tickets exceeding SLA resolution threshold
            </p>
          </div>
        </div>

        {/* Configured SLA Targets per Priority */}
        <div>
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mb-2">
            Configured SLA Targets
          </p>
          {data.policies.length === 0 ? (
            <p className="text-xs text-slate-400 py-2">No active SLA policies configured</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-slate-100 dark:border-slate-800 text-slate-400 font-medium">
                  <tr>
                    <th className="pb-2">Priority</th>
                    <th className="pb-2">Response Target</th>
                    <th className="pb-2">Resolution Target</th>
                    <th className="pb-2 text-right">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {data.policies.map((p) => (
                    <tr key={p.id}>
                      <td className="py-2 font-medium text-slate-800 dark:text-slate-200">
                        {p.priority}
                      </td>
                      <td className="py-2 text-slate-600 dark:text-slate-400">
                        {formatMins(p.firstResponseMinutes)}
                      </td>
                      <td className="py-2 text-slate-600 dark:text-slate-400">
                        {formatMins(p.resolutionMinutes)}
                      </td>
                      <td className="py-2 text-right">
                        <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400">
                          Active
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
