'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { getInitials } from '@/lib/utils';
import {
  LogOut,
  Menu,
  Shield,
  Headphones,
  User as UserIcon,
} from 'lucide-react';

interface NavbarProps {
  onToggleSidebar?: () => void;
  isSidebarOpen?: boolean;
}

export function Navbar({ onToggleSidebar }: NavbarProps) {
  const { user, logout } = useAuth();

  const getRoleBadgeVariant = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return 'danger';
      case 'AGENT':
        return 'secondary';
      default:
        return 'default';
    }
  };

  const getRoleIcon = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return <Shield className="w-3 h-3" />;
      case 'AGENT':
        return <Headphones className="w-3 h-3" />;
      default:
        return <UserIcon className="w-3 h-3" />;
    }
  };

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-200/80 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md px-4 sm:px-6">
      <div className="flex items-center gap-3">
        {onToggleSidebar && (
          <button
            onClick={onToggleSidebar}
            className="p-2 rounded-lg text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 md:hidden"
            aria-label="Toggle navigation"
          >
            <Menu className="w-5 h-5" />
          </button>
        )}
        <Link href="/" className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-white font-bold shadow-md shadow-indigo-500/20">
            S
          </div>
          <div className="flex flex-col">
            <span className="text-base font-bold tracking-tight text-slate-900 dark:text-white">
              Smart<span className="text-indigo-600 dark:text-indigo-400">Desk</span>
            </span>
            <span className="text-[10px] text-slate-500 font-medium tracking-wide uppercase -mt-1">
              Support Ops
            </span>
          </div>
        </Link>
      </div>

      <div className="flex items-center gap-3">
        {user ? (
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex flex-col items-end text-right">
              <span className="text-xs font-semibold text-slate-900 dark:text-slate-100">
                {user.firstName} {user.lastName}
              </span>
              <div className="flex items-center gap-1.5 mt-0.5">
                <Badge variant={getRoleBadgeVariant(user.role)} className="text-[10px] py-0 px-1.5">
                  <span className="flex items-center gap-1">
                    {getRoleIcon(user.role)}
                    {user.role}
                  </span>
                </Badge>
              </div>
            </div>

            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-100 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 font-bold text-xs border border-indigo-200 dark:border-indigo-800">
              {getInitials(user.firstName, user.lastName)}
            </div>

            <button
              onClick={() => logout()}
              title="Sign Out"
              className="p-2 rounded-lg text-slate-500 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span className="sr-only">Log out</span>
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link href="/login">
              <Button variant="ghost" size="sm">
                Sign In
              </Button>
            </Link>
            <Link href="/register">
              <Button size="sm">Get Started</Button>
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
