'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  Ticket,
  PlusCircle,
  Users,
  Shield,
  Layers,
  Tag,
  GitBranch,
  AlertOctagon,
  Settings,
  User,
  Headphones,
  X,
} from 'lucide-react';

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { user } = useAuth();

  const getNavLinks = () => {
    switch (user?.role) {
      case 'ADMIN':
        return [
          { href: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
          { href: '/admin/tickets', label: 'Tickets', icon: Ticket },
          { href: '/admin/customers', label: 'Customers', icon: Users },
          { href: '/admin/agents', label: 'Agents', icon: Headphones },
          { href: '/admin/teams', label: 'Teams', icon: Layers },
          { href: '/admin/categories', label: 'Categories', icon: Tag },
          { href: '/admin/routing-rules', label: 'Routing Rules', icon: GitBranch },
          { href: '/admin/escalation-rules', label: 'Escalation Rules', icon: AlertOctagon },
          { href: '/admin/settings', label: 'Settings', icon: Settings },
        ];
      case 'AGENT':
        return [
          { href: '/agent/dashboard', label: 'Dashboard', icon: LayoutDashboard },
          { href: '/agent/tickets', label: 'My Queue', icon: Ticket },
          { href: '/agent/escalations', label: 'Escalations', icon: AlertOctagon },
          { href: '/agent/profile', label: 'My Profile', icon: User },
        ];
      case 'CUSTOMER':
      default:
        return [
          { href: '/customer/dashboard', label: 'Dashboard', icon: LayoutDashboard },
          { href: '/customer/tickets', label: 'My Tickets', icon: Ticket },
          { href: '/customer/tickets/new', label: 'Create Ticket', icon: PlusCircle },
          { href: '/customer/profile', label: 'My Profile', icon: User },
        ];
    }
  };

  const navLinks = getNavLinks();

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-xs md:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar aside */}
      <aside
        className={cn(
          'fixed top-16 bottom-0 left-0 z-40 w-64 border-r border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 transition-transform duration-200 md:static md:translate-x-0 flex flex-col',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center justify-between p-4 md:hidden border-b border-slate-100 dark:border-slate-800">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Navigation
          </span>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
          <div className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            {user?.role || 'Portal'} Menu
          </div>
          {navLinks.map((link) => {
            const Icon = link.icon;
            const isActive = pathname === link.href || (link.href !== '/' && pathname.startsWith(link.href) && link.href.split('/').length > 2);

            return (
              <Link
                key={link.href}
                href={link.href}
                onClick={onClose}
                className={cn(
                  'flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-lg transition-colors',
                  isActive
                    ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 font-semibold'
                    : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-100'
                )}
              >
                <Icon className={cn('w-4 h-4 shrink-0', isActive ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400')} />
                {link.label}
              </Link>
            );
          })}
        </div>

        {/* User preview footer */}
        {user && (
          <div className="p-3 border-t border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2.5 p-2 rounded-lg bg-slate-50 dark:bg-slate-800/60">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-white text-xs font-bold shrink-0">
                {user.role === 'ADMIN' ? <Shield className="w-4 h-4" /> : user.firstName.charAt(0)}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-slate-900 dark:text-slate-100 truncate">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                  {user.email}
                </p>
              </div>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}
