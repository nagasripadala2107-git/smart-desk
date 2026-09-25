import React, { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
  breadcrumbs?: { label: string; href?: string }[];
  className?: string;
}

export function PageHeader({
  title,
  description,
  actions,
  breadcrumbs,
  className,
}: PageHeaderProps) {
  return (
    <div className={cn('mb-6 space-y-2', className)}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <nav className="flex items-center space-x-1.5 text-xs text-slate-500 dark:text-slate-400 mb-1">
          {breadcrumbs.map((b, i) => (
            <React.Fragment key={i}>
              {i > 0 && <span>/</span>}
              {b.href ? (
                <a href={b.href} className="hover:text-slate-800 dark:hover:text-slate-200 hover:underline">
                  {b.label}
                </a>
              ) : (
                <span className="text-slate-900 dark:text-slate-100 font-medium">{b.label}</span>
              )}
            </React.Fragment>
          ))}
        </nav>
      )}

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
            {title}
          </h1>
          {description && (
            <p className="mt-1 text-xs sm:text-sm text-slate-500 dark:text-slate-400">
              {description}
            </p>
          )}
        </div>
        {actions && <div className="flex items-center gap-2.5 shrink-0">{actions}</div>}
      </div>
    </div>
  );
}
