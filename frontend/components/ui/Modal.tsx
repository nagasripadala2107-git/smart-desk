'use client';

import React, { useEffect, ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  children: ReactNode;
  className?: string;
}

export function Modal({ isOpen, onClose, title, description, children, className }: ModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.style.overflow = 'unset';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity" onClick={onClose} />
      <div
        className={cn(
          'relative z-10 w-full max-w-lg rounded-2xl bg-white dark:bg-slate-900 shadow-2xl border border-slate-200 dark:border-slate-800 p-6 sm:p-7 overflow-hidden',
          className
        )}
      >
        <button
          onClick={onClose}
          className="absolute right-4 top-4 p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
          <span className="sr-only">Close</span>
        </button>
        {title && <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{title}</h2>}
        {description && <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 mb-4">{description}</p>}
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}
