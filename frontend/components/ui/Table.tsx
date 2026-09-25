import React, { HTMLAttributes, TableHTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export function Table({ className, ...props }: TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-auto">
      <table className={cn('w-full caption-bottom text-sm', className)} {...props} />
    </div>
  );
}

export function TableHeader({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn('border-b border-slate-200 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-900/70', className)} {...props} />;
}

export function TableBody({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn('[&_tr:last-child]:border-0', className)} {...props} />;
}

export function TableRow({ className, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn(
        'border-b border-slate-100 dark:border-slate-800/80 transition-colors hover:bg-slate-50/50 dark:hover:bg-slate-800/50 data-[state=selected]:bg-slate-100',
        className
      )}
      {...props}
    />
  );
}

export function TableHead({ className, ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn(
        'h-10 px-4 text-left align-middle font-medium text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400 [&:has([role=checkbox])]:pr-0',
        className
      )}
      {...props}
    />
  );
}

export function TableCell({ className, ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td
      className={cn('p-4 align-middle text-slate-700 dark:text-slate-300 [&:has([role=checkbox])]:pr-0', className)}
      {...props}
    />
  );
}
