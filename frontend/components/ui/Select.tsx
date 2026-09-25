import React, { SelectHTMLAttributes, forwardRef } from 'react';
import { cn } from '@/lib/utils';

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  error?: string;
  label?: string;
  options?: { value: string; label: string }[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, error, label, options, children, id, ...props }, ref) => {
    const selectId = id || props.name;

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label htmlFor={selectId} className="block text-xs font-medium text-slate-700 dark:text-slate-300">
            {label}
          </label>
        )}
        <select
          id={selectId}
          ref={ref}
          className={cn(
            'flex h-9 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-1.5 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors disabled:cursor-not-allowed disabled:opacity-50',
            error && 'border-rose-500 focus:ring-rose-500',
            className
          )}
          {...props}
        >
          {options
            ? options.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))
            : children}
        </select>
        {error && <p className="text-xs text-rose-500">{error}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';
