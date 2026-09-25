'use client';

import React from 'react';
import { DashboardShell } from '@/components/layout/DashboardShell';

export default function AgentLayout({ children }: { children: React.ReactNode }) {
  return <DashboardShell allowedRole="AGENT">{children}</DashboardShell>;
}
