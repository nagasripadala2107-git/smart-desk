'use client';

import React from 'react';
import {
  User,
  FilePlus,
  Cpu,
  GitMerge,
  Headphones,
  CheckCircle2,
  ArrowRight,
} from 'lucide-react';

export function LifecycleDiagram() {
  const steps = [
    {
      title: 'Customer',
      desc: 'Submits inquiry or issue',
      icon: User,
      color: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900',
    },
    {
      title: 'Ticket',
      desc: 'Validated & stored',
      icon: FilePlus,
      color: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border-indigo-200 dark:border-indigo-900',
    },
    {
      title: 'Classification',
      desc: 'AI categorization & priority',
      icon: Cpu,
      color: 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-200 dark:border-purple-900',
    },
    {
      title: 'Routing',
      desc: 'Matches optimal team & agent',
      icon: GitMerge,
      color: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-900',
    },
    {
      title: 'Agent',
      desc: 'Investigates & responds',
      icon: Headphones,
      color: 'bg-teal-500/10 text-teal-600 dark:text-teal-400 border-teal-200 dark:border-teal-900',
    },
    {
      title: 'Resolution',
      desc: 'Resolved & SLA closed',
      icon: CheckCircle2,
      color: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-200 dark:border-emerald-900',
    },
  ];

  return (
    <div className="w-full py-8">
      <div className="grid grid-cols-1 md:grid-cols-6 gap-4 relative">
        {steps.map((step, idx) => {
          const Icon = step.icon;
          return (
            <div key={idx} className="relative flex flex-col items-center text-center group">
              <div
                className={`w-14 h-14 rounded-2xl border flex items-center justify-center mb-3 shadow-xs transition-transform group-hover:scale-105 ${step.color}`}
              >
                <Icon className="w-7 h-7" />
              </div>
              <span className="text-xs font-semibold text-slate-900 dark:text-slate-100">
                {step.title}
              </span>
              <span className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 max-w-[130px]">
                {step.desc}
              </span>

              {/* Connecting arrow for desktop */}
              {idx < steps.length - 1 && (
                <div className="hidden md:block absolute top-7 -right-2 transform translate-x-1/2 text-slate-300 dark:text-slate-700">
                  <ArrowRight className="w-4 h-4" />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
