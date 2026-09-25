import React, { ReactNode } from 'react';
import { Card, CardContent } from '@/components/ui/Card';
import { cn } from '@/lib/utils';

interface StatCardProps {
  title: string;
  value: string | number;
  description?: string;
  icon: ReactNode;
  trend?: {
    value: string;
    isPositive?: boolean;
  };
  className?: string;
}

export function StatCard({ title, value, description, icon, trend, className }: StatCardProps) {
  return (
    <Card className={cn('relative overflow-hidden transition-all hover:shadow-md', className)}>
      <CardContent className="p-5">
        <div className="flex items-center justify-between">
          <p className="text-xs font-medium uppercase tracking-wider text-slate-500 dark:text-slate-400">
            {title}
          </p>
          <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
            {icon}
          </div>
        </div>
        <div className="mt-2">
          <div className="text-2xl font-bold text-slate-900 dark:text-slate-100">{value}</div>
          {description && (
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{description}</p>
          )}
          {trend && (
            <div className="mt-2 flex items-center text-xs font-medium">
              <span className={trend.isPositive ? 'text-emerald-600' : 'text-rose-600'}>
                {trend.value}
              </span>
              <span className="ml-1 text-slate-400">vs last period</span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
