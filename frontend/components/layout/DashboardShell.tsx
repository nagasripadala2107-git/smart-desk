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

export function DashboardShell({ children, allowedRole }: DashboardShellProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const { user, isLoading, isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        router.push('/login');
      } else if (allowedRole && user && user.role !== allowedRole && user.role !== 'ADMIN') {
        // Redirect to the user's role dashboard if trying to access unauthorized role area
        if (user.role === 'CUSTOMER') router.push('/customer/dashboard');
        else if (user.role === 'AGENT') router.push('/agent/dashboard');
        else if (user.role === 'ADMIN') router.push('/admin/dashboard');
      }
    }
  }, [isLoading, isAuthenticated, user, allowedRole, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950">
        <Navbar />
        <div className="max-w-7xl mx-auto p-6 sm:p-8">
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
