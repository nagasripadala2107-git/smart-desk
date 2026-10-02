'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import { TicketSummaryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { EscalationPathDiagram } from '@/components/escalation/EscalationPathDiagram';
import { TicketTable } from '@/components/tickets/TicketTable';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import {
  ShieldAlert,
  Clock,
  CheckCircle,
  AlertTriangle,
  GitBranch,
  Layers,
  ArrowRight,
  RefreshCw,
} from 'lucide-react';

interface EscalationRule {
  id: string;
  name: string;
  triggerCondition: string;
  fromTier: string;
  toTier: string;
  targetTeam: string;
  slaLimit: string;
  isActive: boolean;
}

const DEFAULT_RULES: EscalationRule[] = [
  {
    id: 'rule-1',
    name: 'SLA Breach Auto-Escalation',
    triggerCondition: 'Open ticket exceeding 4h response window without agent reply',
    fromTier: 'Tier 1 (Frontline)',
    toTier: 'Tier 2 (Specialist)',
    targetTeam: 'Technical Support',
    slaLimit: '4 Hours',
    isActive: true,
  },
  {
    id: 'rule-2',
    name: 'High Frustration / Anger Sentiment',
    triggerCondition: 'AI sentiment detection identifies ANGRY or FRUSTRATED customer tone',
    fromTier: 'Tier 1 (Frontline)',
    toTier: 'Tier 2 (Specialist)',
    targetTeam: 'Billing & Account Specialists',
    slaLimit: '2 Hours',
    isActive: true,
  },
  {
    id: 'rule-3',
    name: 'Urgent Infrastructure Outage',
    triggerCondition: 'Category = TECHNICAL and Priority = URGENT with defect severity',
    fromTier: 'Tier 2 (Specialist)',
    toTier: 'Tier 3 (Senior Support)',
    targetTeam: 'Senior Support',
    slaLimit: '1 Hour',
    isActive: true,
  },
  {
    id: 'rule-4',
    name: 'Security Vulnerability & Incident Protocol',
    triggerCondition: 'Category = SECURITY or customer reports potential compromise',
    fromTier: 'Any Tier',
    toTier: 'Tier 4 (Incident Command)',
    targetTeam: 'Security & Management',
    slaLimit: '30 Minutes',
    isActive: true,
  },
  {
    id: 'rule-5',
    name: 'Repeat Unresolved Query Escalation',
    triggerCondition: 'More than 3 customer replies without ticket resolution',
    fromTier: 'Tier 1 (Frontline)',
    toTier: 'Tier 2 (Specialist)',
    targetTeam: 'Senior Support',
    slaLimit: '2 Hours',
    isActive: true,
  },
];

export default function AdminEscalationRulesPage() {
  const [escalatedTickets, setEscalatedTickets] = useState<TicketSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [rules, setRules] = useState<EscalationRule[]>(DEFAULT_RULES);

  const fetchTickets = async () => {
    setIsLoading(true);
    try {
      const all = await api.get<TicketSummaryResponse[]>('/tickets');
      setEscalatedTickets(all.filter((t) => t.status === 'ESCALATED'));
    } catch {
      // Fallback if offline
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTickets();
  }, []);

  const toggleRule = (id: string) => {
    setRules((prev) =>
      prev.map((r) => (r.id === id ? { ...r, isActive: !r.isActive } : r))
    );
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      <PageHeader
        title="Escalation Policies & SLA Architecture"
        description="Configure multi-tier escalation thresholds, deterministic DAG chains, and automated breach transfer triggers."
        actions={
          <Button variant="outline" size="sm" onClick={fetchTickets} className="gap-1.5">
            <RefreshCw className="w-3.5 h-3.5" />
            Refresh Active Escalations
          </Button>
        }
      />

      {/* Visual Escalation Path Progression Component */}
      <EscalationPathDiagram />

      {/* Active Escalation Rules Table */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <div>
            <CardTitle className="text-base font-bold flex items-center gap-2">
              <GitBranch className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
              Active Escalation Trigger Policies
            </CardTitle>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              Rules executed by the SmartDesk routing engine when conditions or SLA thresholds are met.
            </p>
          </div>
          <Badge variant="outline" className="font-mono text-xs">
            {rules.filter((r) => r.isActive).length} / {rules.length} Active
          </Badge>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 dark:text-slate-400 font-semibold border-y border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="py-3 px-4">Policy Name</th>
                  <th className="py-3 px-4">Trigger Condition</th>
                  <th className="py-3 px-4">Path Progression</th>
                  <th className="py-3 px-4">Target Team</th>
                  <th className="py-3 px-4">SLA Window</th>
                  <th className="py-3 px-4 text-right">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {rules.map((rule) => (
                  <tr key={rule.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-slate-100">
                      {rule.name}
                    </td>
                    <td className="py-3.5 px-4 text-slate-600 dark:text-slate-300 max-w-xs">
                      {rule.triggerCondition}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center gap-1.5 font-medium text-slate-700 dark:text-slate-300">
                        <span>{rule.fromTier}</span>
                        <ArrowRight className="w-3 h-3 text-slate-400" />
                        <span className="text-indigo-600 dark:text-indigo-400 font-bold">{rule.toTier}</span>
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-medium text-slate-800 dark:text-slate-200">
                      {rule.targetTeam}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-medium text-amber-600 dark:text-amber-400">
                      {rule.slaLimit}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => toggleRule(rule.id)}
                        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-semibold transition-colors ${
                          rule.isActive
                            ? 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800'
                            : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
                        }`}
                      >
                        {rule.isActive ? 'Active' : 'Disabled'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Live Escalated Tickets Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-rose-600" />
              Live Escalated Tickets Queue
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Tickets currently transferred to specialist or executive tiers for rapid resolution.
            </p>
          </div>
          <Badge variant="danger" className="text-xs font-mono">
            {escalatedTickets.length} Escalated
          </Badge>
        </div>

        {escalatedTickets.length === 0 ? (
          <div className="p-8 rounded-xl border border-dashed border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30 text-center space-y-2">
            <CheckCircle className="w-8 h-8 text-emerald-500 mx-auto" />
            <h4 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              No Escalation Bottlenecks
            </h4>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
              All active tickets are within their primary resolution windows.
            </p>
          </div>
        ) : (
          <TicketTable tickets={escalatedTickets} basePath="/admin/tickets" showCustomer={true} />
        )}
      </div>
    </div>
  );
}
