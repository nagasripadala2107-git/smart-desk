'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Send, Lock } from 'lucide-react';

interface MessageComposerProps {
  onSendMessage: (message: string, isInternal: boolean) => Promise<void>;
  allowInternalNotes?: boolean;
}

export function MessageComposer({
  onSendMessage,
  allowInternalNotes = false,
}: MessageComposerProps) {
  const [message, setMessage] = useState('');
  const [isInternal, setIsInternal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim()) return;

    setIsSubmitting(true);
    setError(null);
    try {
      await onSendMessage(message.trim(), isInternal);
      setMessage('');
      setIsInternal(false);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to send message');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {error && <p className="text-xs text-rose-600 dark:text-rose-400">{error}</p>}
      <div className="relative">
        <textarea
          rows={3}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder={
            isInternal
              ? 'Add an internal note visible only to support agents...'
              : 'Write a reply to the customer...'
          }
          className={`w-full rounded-xl p-3 text-xs sm:text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 border transition-colors focus:outline-none focus:ring-2 focus:border-transparent resize-y min-h-[90px] ${
            isInternal
              ? 'border-amber-300 dark:border-amber-800/80 bg-amber-50/30 dark:bg-amber-950/20 focus:ring-amber-500'
              : 'border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 focus:ring-indigo-500'
          }`}
        />
      </div>

      <div className="flex items-center justify-between">
        {allowInternalNotes ? (
          <label className="flex items-center gap-2 cursor-pointer select-none text-xs text-slate-600 dark:text-slate-400">
            <input
              type="checkbox"
              checked={isInternal}
              onChange={(e) => setIsInternal(e.target.checked)}
              className="rounded border-slate-300 text-amber-600 focus:ring-amber-500 w-3.5 h-3.5"
            />
            <span className="flex items-center gap-1 font-medium text-amber-700 dark:text-amber-400">
              <Lock className="w-3 h-3" />
              Internal Note (Invisible to Customer)
            </span>
          </label>
        ) : (
          <div />
        )}

        <Button
          type="submit"
          size="sm"
          disabled={!message.trim() || isSubmitting}
          isLoading={isSubmitting}
          variant={isInternal ? 'secondary' : 'primary'}
          className="gap-1.5"
        >
          <Send className="w-3.5 h-3.5" />
          {isInternal ? 'Save Internal Note' : 'Send Reply'}
        </Button>
      </div>
    </form>
  );
}
