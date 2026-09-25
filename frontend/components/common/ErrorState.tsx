import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/Button';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({
  title = 'Something went wrong',
  message = 'Unable to load data from the server. Please try again.',
  onRetry,
  className = '',
}: ErrorStateProps) {
  return (
    <div
      className={`flex flex-col items-center justify-center p-8 text-center rounded-xl border border-rose-200 dark:border-rose-900/50 bg-rose-50/40 dark:bg-rose-950/20 my-4 ${className}`}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 dark:bg-rose-900/40 text-rose-600 dark:text-rose-400 mb-3">
        <AlertTriangle className="w-6 h-6" />
      </div>
      <h4 className="text-sm font-semibold text-rose-900 dark:text-rose-200">{title}</h4>
      <p className="mt-1 text-xs text-rose-600/90 dark:text-rose-300/80 max-w-sm">{message}</p>
      {onRetry && (
        <div className="mt-4">
          <Button variant="outline" size="sm" onClick={onRetry} className="gap-1.5">
            <RefreshCw className="w-3.5 h-3.5" />
            Try Again
          </Button>
        </div>
      )}
    </div>
  );
}
