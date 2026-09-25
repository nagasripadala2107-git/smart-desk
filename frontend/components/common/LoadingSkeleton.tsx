import React from 'react';
import { Skeleton } from '@/components/ui/Skeleton';

interface LoadingSkeletonProps {
  rows?: number;
  type?: 'table' | 'card' | 'details';
}

export function LoadingSkeleton({ rows = 5, type = 'table' }: LoadingSkeletonProps) {
  if (type === 'card') {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="p-5 border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 space-y-3">
            <div className="flex justify-between items-center">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-8 w-8 rounded-lg" />
            </div>
            <Skeleton className="h-7 w-16" />
            <Skeleton className="h-3 w-32" />
          </div>
        ))}
      </div>
    );
  }

  if (type === 'details') {
    return (
      <div className="space-y-6">
        <div className="p-6 border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 space-y-4">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
          <div className="flex gap-3 pt-2">
            <Skeleton className="h-6 w-20 rounded-full" />
            <Skeleton className="h-6 w-20 rounded-md" />
          </div>
        </div>
        <div className="p-6 border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 space-y-3">
          <Skeleton className="h-5 w-32" />
          <Skeleton className="h-16 w-full rounded-lg" />
          <Skeleton className="h-16 w-full rounded-lg" />
        </div>
      </div>
    );
  }

  return (
    <div className="w-full border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 p-4 space-y-3">
      <div className="flex justify-between items-center pb-2 border-b border-slate-100 dark:border-slate-800">
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-8 w-24 rounded-lg" />
      </div>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex items-center space-x-4 py-2">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-4 w-48 flex-1" />
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-4 w-16" />
          <Skeleton className="h-4 w-24" />
        </div>
      ))}
    </div>
  );
}
