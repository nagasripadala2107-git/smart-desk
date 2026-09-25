'use client';

import React from 'react';
import { DashboardShell } from '@/components/layout/DashboardShell';

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  return <DashboardShell allowedRole="CUSTOMER">{children}</DashboardShell>;
}
