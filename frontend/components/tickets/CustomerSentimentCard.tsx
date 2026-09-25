'use client';

import React from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import {
  HeartHandshake,
  Smile,
  Meh,
  Frown,
  AlertTriangle,
  Zap,
  HelpCircle,
  CheckCircle2,
  Shield,
  Info,
} from 'lucide-react';

interface CustomerSentimentCardProps {
  sentiment?: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | null;
  confidence?: number | null;
  tone?: string | null;
  modelVersion?: string | null;
}

export function CustomerSentimentCard({
  sentiment,
  confidence,
  tone,
  modelVersion,
}: CustomerSentimentCardProps) {
  if (!sentiment && !tone) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-1.5">
            <HeartHandshake className="w-4 h-4 text-slate-400" />
            <CardTitle className="text-sm">Customer Sentiment</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <p className="text-xs text-slate-500 italic">
            Sentiment analysis is unavailable or pending for this ticket.
          </p>
        </CardContent>
      </Card>
    );
  }

  // Sentiment presentation config
  const sentimentConfig = {
    POSITIVE: {
      label: 'Positive',
      variant: 'success' as const,
      icon: Smile,
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-300 dark:border-emerald-800',
      barColor: 'bg-emerald-500',
    },
    NEUTRAL: {
      label: 'Neutral',
      variant: 'default' as const,
      icon: Meh,
      badgeClass: 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
      barColor: 'bg-slate-500',
    },
    NEGATIVE: {
      label: 'Negative',
      variant: 'danger' as const,
      icon: Frown,
      badgeClass: 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/50 dark:text-rose-300 dark:border-rose-800',
      barColor: 'bg-rose-500',
    },
  };

  const currentSentiment = sentiment ? sentimentConfig[sentiment] : sentimentConfig.NEUTRAL;
  const SentimentIcon = currentSentiment.icon;

  // Tone presentation config
  const getToneBadge = (toneVal?: string | null) => {
    if (!toneVal) return null;
    const normalized = toneVal.toUpperCase();

    switch (normalized) {
      case 'ANGRY':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 border border-red-200 dark:bg-red-950/50 dark:text-red-300 dark:border-red-800">
            <AlertTriangle className="w-3 h-3 text-red-600 dark:text-red-400" />
            Angry
          </span>
        );
      case 'URGENT':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800 border border-purple-200 dark:bg-purple-950/50 dark:text-purple-300 dark:border-purple-800">
            <Zap className="w-3 h-3 text-purple-600 dark:text-purple-400" />
            Urgent
          </span>
        );
      case 'FRUSTRATED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800 border border-amber-200 dark:bg-amber-950/50 dark:text-amber-300 dark:border-amber-800">
            <AlertTriangle className="w-3 h-3 text-amber-600 dark:text-amber-400" />
            Frustrated
          </span>
        );
      case 'CONFUSED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-sky-100 text-sky-800 border border-sky-200 dark:bg-sky-950/50 dark:text-sky-300 dark:border-sky-800">
            <HelpCircle className="w-3 h-3 text-sky-600 dark:text-sky-400" />
            Confused
          </span>
        );
      case 'SATISFIED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800 border border-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-300 dark:border-emerald-800">
            <CheckCircle2 className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
            Satisfied
          </span>
        );
      case 'CALM':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-teal-100 text-teal-800 border border-teal-200 dark:bg-teal-950/50 dark:text-teal-300 dark:border-teal-800">
            <Shield className="w-3 h-3 text-teal-600 dark:text-teal-400" />
            Calm
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700">
            {toneVal}
          </span>
        );
    }
  };

  const confidencePct = confidence != null ? (confidence * 100).toFixed(1) : null;

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <HeartHandshake className="w-4 h-4 text-indigo-500" />
            <CardTitle className="text-sm">Customer Sentiment</CardTitle>
          </div>
          {modelVersion && (
            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-500">
              {modelVersion}
            </span>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-3.5 text-xs">
        {/* Sentiment & Tone Row */}
        <div className="flex items-center justify-between">
          <span className="text-slate-500">Predicted Sentiment:</span>
          {sentiment ? (
            <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border ${currentSentiment.badgeClass}`}>
              <SentimentIcon className="w-3.5 h-3.5" />
              {currentSentiment.label}
            </span>
          ) : (
            <span className="text-slate-400 italic">Not available</span>
          )}
        </div>

        {/* Tone Row */}
        {tone && (
          <div className="flex items-center justify-between">
            <span className="text-slate-500">Customer Tone:</span>
            {getToneBadge(tone)}
          </div>
        )}

        {/* Confidence Meter */}
        {confidencePct != null && (
          <div className="space-y-1.5 pt-1 border-t border-slate-100 dark:border-slate-800">
            <div className="flex items-center justify-between text-slate-500">
              <span>Model Confidence:</span>
              <span className="font-mono font-medium text-slate-800 dark:text-slate-200">
                {confidencePct}%
              </span>
            </div>
            <div className="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full ${currentSentiment.barColor}`}
                style={{ width: `${Math.min(Math.max(Number(confidencePct), 5), 100)}%` }}
              />
            </div>
          </div>
        )}

        {/* Advisory Disclaimer */}
        <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-start gap-1.5 text-[11px] text-slate-500 dark:text-slate-400">
          <Info className="w-3.5 h-3.5 mt-0.5 flex-shrink-0 text-slate-400" />
          <span>
            Advisory context only. Human staff retains complete operational authority.
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
