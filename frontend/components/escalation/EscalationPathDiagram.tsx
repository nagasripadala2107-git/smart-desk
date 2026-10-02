'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import {
  ShieldAlert,
  ArrowRight,
  Clock,
  Layers,
  AlertTriangle,
  CheckCircle2,
  Users,
  Cpu,
  Flame,
  Crown,
} from 'lucide-react';

interface TierInfo {
  level: number;
  name: string;
  targetTeam: string;
  sla: string;
  slaMinutes: number;
  description: string;
  triggers: string[];
  color: string;
  borderColor: string;
  bgColor: string;
  icon: React.ElementType;
}

const ESCALATION_TIERS: TierInfo[] = [
  {
    level: 1,
    name: 'Tier 1: Frontline Triage',
    targetTeam: 'General Support',
    sla: '4h Response SLA',
    slaMinutes: 240,
    description: 'Initial intake, classification via local AI model, and customer validation.',
    triggers: ['New ticket submitted', 'AI category confidence >= 70%'],
    color: 'text-blue-600 dark:text-blue-400',
    borderColor: 'border-blue-200 dark:border-blue-800',
    bgColor: 'bg-blue-50/60 dark:bg-blue-950/30',
    icon: Users,
  },
  {
    level: 2,
    name: 'Tier 2: Specialist Routing',
    targetTeam: 'Technical Support / Billing',
    sla: '2h Response SLA',
    slaMinutes: 120,
    description: 'Domain-specific investigation, reproducible defects, and account adjustments.',
    triggers: ['Domain rule match', 'Agent manual handoff', 'Frustration sentiment'],
    color: 'text-indigo-600 dark:text-indigo-400',
    borderColor: 'border-indigo-200 dark:border-indigo-800',
    bgColor: 'bg-indigo-50/60 dark:bg-indigo-950/30',
    icon: Cpu,
  },
  {
    level: 3,
    name: 'Tier 3: Senior Engineering',
    targetTeam: 'Senior Support',
    sla: '1h Response SLA',
    slaMinutes: 60,
    description: 'Deep root-cause diagnostics, database repairs, and code-level hotfixes.',
    triggers: ['Urgent priority + Bug', 'Tier 2 blocker > 90m', 'High complexity flag'],
    color: 'text-amber-600 dark:text-amber-400',
    borderColor: 'border-amber-200 dark:border-amber-800',
    bgColor: 'bg-amber-50/60 dark:bg-amber-950/30',
    icon: Flame,
  },
  {
    level: 4,
    name: 'Tier 4: Executive Incident Command',
    targetTeam: 'Management & Security',
    sla: '30m Breach SLA',
    slaMinutes: 30,
    description: 'Executive notification, compliance mitigation, and high-severity customer comms.',
    triggers: ['SLA countdown breach', 'Security compromise', 'Enterprise client blocker'],
    color: 'text-rose-600 dark:text-rose-400',
    borderColor: 'border-rose-200 dark:border-rose-800',
    bgColor: 'bg-rose-50/60 dark:bg-rose-950/30',
    icon: Crown,
  },
];

export function EscalationPathDiagram() {
  const [selectedTier, setSelectedTier] = useState<TierInfo>(ESCALATION_TIERS[0]);

  return (
    <Card className="border-indigo-100 dark:border-indigo-950/60 shadow-sm overflow-hidden">
      <CardHeader className="bg-slate-50/70 dark:bg-slate-900/40 border-b border-slate-100 dark:border-slate-800 pb-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-lg bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400">
                <ShieldAlert className="w-4 h-4" />
              </span>
              <CardTitle className="text-base font-bold text-slate-900 dark:text-slate-100">
                Multi-Tier Escalation Path & SLA Architecture
              </CardTitle>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              Deterministic incident handoff flow governing ticket escalation from intake to executive resolution.
            </p>
          </div>
          <Badge variant="outline" className="text-[11px] self-start sm:self-center font-mono">
            Deterministic DAG Flow
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="p-6 space-y-6">
        {/* Tier Cards Progression Pipeline */}
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 relative">
          {ESCALATION_TIERS.map((tier, idx) => {
            const Icon = tier.icon;
            const isSelected = selectedTier.level === tier.level;

            return (
              <div
                key={tier.level}
                onClick={() => setSelectedTier(tier)}
                className={`relative cursor-pointer rounded-xl p-4 border transition-all duration-200 ${
                  isSelected
                    ? `${tier.borderColor} ring-2 ring-indigo-500/20 shadow-md ${tier.bgColor}`
                    : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                {/* Step Pill */}
                <div className="flex items-center justify-between mb-3">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold tracking-wider uppercase bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                    Tier {tier.level}
                  </span>
                  <div className="flex items-center gap-1 text-[11px] font-mono font-semibold text-slate-500 dark:text-slate-400">
                    <Clock className="w-3.5 h-3.5" />
                    <span>{tier.sla}</span>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className={`p-2 rounded-lg ${tier.bgColor} ${tier.color} shrink-0`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 leading-tight">
                      {tier.name.split(':')[1].trim()}
                    </h4>
                    <p className="text-[11px] font-medium text-indigo-600 dark:text-indigo-400 mt-0.5">
                      {tier.targetTeam}
                    </p>
                  </div>
                </div>

                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-3 line-clamp-2 leading-relaxed">
                  {tier.description}
                </p>

                {/* Arrow indicator for pipeline flow */}
                {idx < ESCALATION_TIERS.length - 1 && (
                  <div className="hidden xl:block absolute -right-3 top-1/2 -translate-y-1/2 z-10 p-1 rounded-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-400">
                    <ArrowRight className="w-3 h-3" />
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Selected Tier Deep-Dive Details */}
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/60 p-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-200/80 dark:border-slate-800">
            <div>
              <span className="text-[10px] uppercase font-bold tracking-wider text-indigo-600 dark:text-indigo-400">
                Detailed Policy Parameters
              </span>
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
                {selectedTier.name} — Handled by {selectedTier.targetTeam}
              </h3>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-500">Target SLA:</span>
              <Badge variant="default" className="font-mono text-xs">
                {selectedTier.sla}
              </Badge>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4 text-xs">
            <div>
              <h5 className="font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5 mb-2">
                <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                Trigger Conditions:
              </h5>
              <ul className="space-y-1.5 pl-5 list-disc text-slate-600 dark:text-slate-400">
                {selectedTier.triggers.map((trigger, i) => (
                  <li key={i}>{trigger}</li>
                ))}
              </ul>
            </div>

            <div>
              <h5 className="font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5 mb-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                Operational Guarantees:
              </h5>
              <p className="text-slate-600 dark:text-slate-400 leading-relaxed">
                {selectedTier.description} All state transitions and tier transfers are permanently recorded with
                acting user ID and timestamped metadata in the Postgres audit log.
              </p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
