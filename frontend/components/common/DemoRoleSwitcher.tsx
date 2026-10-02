'use client';

import React, { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Crown, Headphones, User, RefreshCw, ChevronDown } from 'lucide-react';

interface DemoAccount {
  role: 'ADMIN' | 'AGENT' | 'CUSTOMER';
  label: string;
  email: string;
  badge: string;
  color: string;
  icon: React.ElementType;
}

const DEMO_ACCOUNTS: DemoAccount[] = [
  {
    role: 'ADMIN',
    label: 'Admin Portal',
    email: 'admin@smartdesk.local',
    badge: 'System Admin',
    color: 'bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300 border-rose-200 dark:border-rose-800',
    icon: Crown,
  },
  {
    role: 'AGENT',
    label: 'Support Agent',
    email: 'agent.tech@smartdesk.local',
    badge: 'Tier 2 Tech',
    color: 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800',
    icon: Headphones,
  },
  {
    role: 'CUSTOMER',
    label: 'Customer Portal',
    email: 'alex@acmecorp.local',
    badge: 'Acme Corp',
    color: 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300 border-blue-200 dark:border-blue-800',
    icon: User,
  },
];

export function DemoRoleSwitcher() {
  const { user, login } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isSwitching, setIsSwitching] = useState(false);

  const currentAccount = DEMO_ACCOUNTS.find((a) => a.role === user?.role) || DEMO_ACCOUNTS[0];

  const handleSwitch = async (account: DemoAccount) => {
    if (account.role === user?.role || isSwitching) return;
    setIsSwitching(true);
    setIsOpen(false);
    try {
      await login({ email: account.email, password: 'Password123!' });
      window.location.reload();
    } catch (e) {
      console.error('Failed to switch demo role', e);
    } finally {
      setIsSwitching(false);
    }
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        disabled={isSwitching}
        className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl border border-indigo-200 dark:border-indigo-800 bg-indigo-50/70 dark:bg-indigo-950/50 hover:bg-indigo-100/80 transition-all text-xs font-semibold text-indigo-900 dark:text-indigo-200 shadow-xs"
        title="1-Click Reviewer Role Switcher"
      >
        <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
        <span className="hidden sm:inline font-mono text-[11px] uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
          Demo:
        </span>
        <span className="flex items-center gap-1.5">
          <currentAccount.icon className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
          <span>{currentAccount.label}</span>
        </span>
        {isSwitching ? (
          <RefreshCw className="w-3.5 h-3.5 animate-spin text-indigo-500" />
        ) : (
          <ChevronDown className="w-3 h-3 text-slate-400" />
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-xl z-50 p-2 space-y-1">
          <div className="px-3 py-2 border-b border-slate-100 dark:border-slate-800">
            <p className="text-[10px] uppercase font-bold tracking-wider text-indigo-600 dark:text-indigo-400">
              Reviewer Fast-Switch
            </p>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              Switch roles instantly with 1-click (no password required).
            </p>
          </div>

          {DEMO_ACCOUNTS.map((account) => {
            const Icon = account.icon;
            const isCurrent = user?.role === account.role;

            return (
              <button
                key={account.role}
                onClick={() => handleSwitch(account)}
                disabled={isCurrent || isSwitching}
                className={`w-full flex items-center justify-between p-2.5 rounded-xl text-left transition-all ${
                  isCurrent
                    ? 'bg-slate-100 dark:bg-slate-800 cursor-default opacity-80'
                    : 'hover:bg-slate-50 dark:hover:bg-slate-800/60'
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <div className={`p-1.5 rounded-lg border ${account.color}`}>
                    <Icon className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                      {account.label}
                    </div>
                    <div className="text-[10px] text-slate-400 font-mono">
                      {account.email}
                    </div>
                  </div>
                </div>
                {isCurrent && (
                  <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 dark:bg-emerald-950 px-2 py-0.5 rounded-full">
                    Active
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
