import React from 'react';
import { Construction, Sparkles } from 'lucide-react';

interface NotImplementedStateProps {
  featureName: string;
  plannedPhase?: string;
  description?: string;
}

export function NotImplementedState({
  featureName,
  plannedPhase,
  description,
}: NotImplementedStateProps) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center rounded-2xl border border-dashed border-slate-300 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-900/40 my-6">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-100/70 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 mb-4 shadow-xs">
        <Construction className="w-7 h-7" />
      </div>
      <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
        Backend endpoint not available yet
      </h3>
      <p className="mt-1 text-sm font-medium text-indigo-600 dark:text-indigo-400">
        {featureName}
      </p>
      <p className="mt-2 text-xs text-slate-500 dark:text-slate-400 max-w-md">
        {description ||
          'This management module is scheduled for future backend phases. In compliance with strict phase boundaries, no mock persistence or simulated actions are performed.'}
      </p>
      {plannedPhase && (
        <div className="mt-4 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
          <Sparkles className="w-3.5 h-3.5" />
          Scheduled for {plannedPhase}
        </div>
      )}
    </div>
  );
}
