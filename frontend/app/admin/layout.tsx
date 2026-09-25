'use client';

import React from 'react';
import { DashboardShell } from '@/components/layout/DashboardShell';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <DashboardShell allowedRole="ADMIN">{children}</DashboardShell>;
}
