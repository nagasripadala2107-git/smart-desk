'use client';

import React from 'react';
import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Copy, ExternalLink, Info, AlertCircle } from 'lucide-react';
import { DuplicateMatchDto } from '@/types';

interface PotentialDuplicatesCardProps {
  matches?: DuplicateMatchDto[] | null;
}

export function PotentialDuplicatesCard({ matches }: PotentialDuplicatesCardProps) {
  // If duplicate analysis was unavailable (null or undefined)
  if (matches === null || matches === undefined) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center gap-1.5">
            <Copy className="w-4 h-4 text-slate-400" />
            <CardTitle className="text-sm">Potential Duplicate Tickets</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2 text-xs text-slate-500 italic py-1">
            <AlertCircle className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
            <span>Duplicate analysis unavailable.</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  // If duplicate analysis completed with 0 matches
  if (matches.length === 0) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center gap-1.5">
            <Copy className="w-4 h-4 text-slate-400" />
            <CardTitle className="text-sm">Potential Duplicate Tickets</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <p className="text-xs text-slate-500 italic py-1">
            No potential duplicates found.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <Copy className="w-4 h-4 text-amber-500" />
            <CardTitle className="text-sm">Potential Duplicate Tickets</CardTitle>
          </div>
          <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 dark:bg-amber-950/50 dark:text-amber-300 border border-amber-200 dark:border-amber-800">
            {matches.length} {matches.length === 1 ? 'match' : 'matches'}
          </span>
        </div>
      </CardHeader>
      <CardContent className="space-y-3.5 text-xs">
        {/* Advisory Header Note */}
        <div className="text-[11px] text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-900/60 p-2.5 rounded-lg border border-slate-100 dark:border-slate-800">
          <p className="font-medium text-slate-700 dark:text-slate-300 mb-0.5">
            AI-generated similarity signal.
          </p>
          <p>Review before taking action. The human support agent retains complete authority.</p>
        </div>

        {/* Duplicate Matches List */}
        <div className="space-y-3">
          {matches.map((match) => {
            const similarityPercent = (match.similarityScore * 100).toFixed(0);
            return (
              <div
                key={match.ticketId}
                className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 space-y-2.5 hover:border-slate-300 dark:hover:border-slate-700 transition-colors"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="font-mono text-xs font-semibold text-indigo-600 dark:text-indigo-400">
                      {match.ticketNumber}
                    </span>
                    <p className="font-medium text-slate-900 dark:text-slate-100 line-clamp-2 mt-0.5 text-xs">
                      {match.subject}
                    </p>
                  </div>
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-amber-50 text-amber-700 border border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800 flex-shrink-0">
                    {similarityPercent}% match
                  </span>
                </div>

                {/* Similarity Progress Bar */}
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-[10px] text-slate-400">
                    <span>Similarity: {similarityPercent}%</span>
                    {match.modelVersion && (
                      <span className="font-mono text-[10px] text-slate-400">
                        {match.modelVersion}
                      </span>
                    )}
                  </div>
                  <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full bg-amber-500 transition-all duration-300"
                      style={{ width: `${Math.min(Math.max(Number(similarityPercent), 5), 100)}%` }}
                    />
                  </div>
                </div>

                {/* View Ticket Link */}
                <div className="pt-1 flex justify-end">
                  <Link
                    href={`/agent/tickets/${match.ticketId}`}
                    className="inline-flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 dark:hover:text-indigo-300"
                  >
                    View Ticket
                    <ExternalLink className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>

        {/* Advisory Disclaimer */}
        <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-start gap-1.5 text-[11px] text-slate-500 dark:text-slate-400">
          <Info className="w-3.5 h-3.5 mt-0.5 flex-shrink-0 text-slate-400" />
          <span>
            Potential duplicate does not mean confirmed duplicate. Human review is required.
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
