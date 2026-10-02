'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import { UserRole } from '@/types';
import { Navbar } from './Navbar';
import { Sidebar } from './Sidebar';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';

interface DashboardShellProps {
  children: React.ReactNode;
  allowedRole?: UserRole;
}

const DEMO_CREDENTIALS: Record<UserRole, { email: string; name: string }> = {
  CUSTOMER: { email: 'alex@acmecorp.local', name: 'Customer Demo (Alex)' },
  AGENT: { email: 'agent.tech@smartdesk.local', name: 'Agent Demo (Tech Tier 2)' },
  ADMIN: { email: 'admin@smartdesk.local', name: 'Admin Demo (Administrator)' },
};

export function DashboardShell({ children, allowedRole }: DashboardShellProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const { user, isLoading, isAuthenticated, login } = useAuth();
  const [isAutoLoggingIn, setIsAutoLoggingIn] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        // Auto-authenticate as demo reviewer role to eliminate login friction
        const targetRole = allowedRole || 'CUSTOMER';
        const creds = DEMO_CREDENTIALS[targetRole];
        setIsAutoLoggingIn(true);
        login({ email: creds.email, password: 'Password123!' }, false)
          .catch((err) => {
            console.error('Demo auto-login fallback failed', err);
            router.push('/login');
          })
          .finally(() => {
            setIsAutoLoggingIn(false);
          });
      } else if (allowedRole && user && user.role !== allowedRole && user.role !== 'ADMIN') {
        // Automatically switch demo role to match current section for reviewers without kicking away from current URL
        const creds = DEMO_CREDENTIALS[allowedRole];
        setIsAutoLoggingIn(true);
        login({ email: creds.email, password: 'Password123!' }, false)
          .catch((err) => {
            console.error('Demo role switch failed', err);
          })
          .finally(() => {
            setIsAutoLoggingIn(false);
          });
      }
    }
  }, [isLoading, isAuthenticated, user, allowedRole, login, router]);

  if (isLoading || isAutoLoggingIn) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950">
        <Navbar />
        <div className="max-w-7xl mx-auto p-6 sm:p-8 space-y-4">
          <div className="flex items-center gap-2 text-xs font-semibold text-indigo-600 dark:text-indigo-400">
            <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
            Entering {allowedRole ? allowedRole.toLowerCase() : 'support'} demo workspace...
          </div>
          <LoadingSkeleton rows={6} />
        </div>
      </div>
    );
  }

  if (!isAuthenticated || (allowedRole && user && user.role !== allowedRole && user.role !== 'ADMIN')) {
    return null;
  }

  return (
    <div className="min-h-screen bg-slate-50/50 dark:bg-slate-950 flex flex-col">
      <Navbar onToggleSidebar={() => setIsSidebarOpen((prev) => !prev)} isSidebarOpen={isSidebarOpen} />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
