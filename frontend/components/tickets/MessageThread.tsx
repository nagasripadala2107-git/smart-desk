'use client';

import React from 'react';
import { MessageResponse } from '@/types';
import { formatDate, getInitials } from '@/lib/utils';
import { Badge } from '@/components/ui/Badge';
import { Lock, User, Headphones, Shield } from 'lucide-react';

interface MessageThreadProps {
  messages: MessageResponse[];
  currentUserId?: string;
}

export function MessageThread({ messages, currentUserId: _currentUserId }: MessageThreadProps) {
  if (!messages || messages.length === 0) {
    return (
      <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400">
        No conversation messages yet. Send a message below to start the thread.
      </div>
    );
  }

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return <Shield className="w-3 h-3 text-rose-500" />;
      case 'AGENT':
        return <Headphones className="w-3 h-3 text-indigo-500" />;
      default:
        return <User className="w-3 h-3 text-slate-500" />;
    }
  };

  return (
    <div className="space-y-4">
      {messages.map((msg) => {
        const isAgentOrAdmin = msg.senderRole === 'AGENT' || msg.senderRole === 'ADMIN';

        return (
          <div
            key={msg.id}
            className={`flex flex-col ${
              msg.internal
                ? 'bg-amber-50/70 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/60 rounded-xl p-4'
                : isAgentOrAdmin
                ? 'bg-indigo-50/40 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/40 rounded-xl p-4'
                : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4'
            }`}
          >
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800/80 pb-2.5 mb-2.5">
              <div className="flex items-center gap-2.5">
                <div
                  className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                    isAgentOrAdmin
                      ? 'bg-indigo-600 text-white'
                      : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200'
                  }`}
                >
                  {getInitials(msg.senderName)}
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-slate-900 dark:text-slate-100">
                    {msg.senderName}
                  </span>
                  <Badge variant={isAgentOrAdmin ? 'secondary' : 'default'} className="text-[10px] py-0 px-1.5">
                    <span className="flex items-center gap-1">
                      {getRoleIcon(msg.senderRole)}
                      {msg.senderRole}
                    </span>
                  </Badge>
                  {msg.internal && (
                    <Badge variant="warning" className="text-[10px] py-0 px-1.5 gap-1">
                      <Lock className="w-2.5 h-2.5" />
                      Internal Note
                    </Badge>
                  )}
                </div>
              </div>
              <time className="text-[11px] text-slate-400" dateTime={msg.createdAt}>
                {formatDate(msg.createdAt)}
              </time>
            </div>

            <div className="text-xs text-slate-700 dark:text-slate-300 whitespace-pre-wrap leading-relaxed">
              {msg.message}
            </div>
          </div>
        );
      })}
    </div>
  );
}
