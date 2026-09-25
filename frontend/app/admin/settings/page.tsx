'use client';

import React from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { API_BASE_URL } from '@/lib/constants';
import { Server, Shield, Database } from 'lucide-react';

export default function AdminSettingsPage() {
  return (
    <div className="space-y-6 max-w-4xl">
      <PageHeader
        title="System Settings & Architecture"
        description="SmartDesk runtime configuration, microservice connectivity, and security parameters."
      />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Server className="w-4 h-4 text-indigo-600" />
                <CardTitle className="text-sm">Backend API Core</CardTitle>
              </div>
              <Badge variant="success">Online</Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <div className="flex justify-between">
              <span className="text-slate-500">Framework:</span>
              <span className="font-semibold text-slate-800 dark:text-slate-200">Spring Boot 4.1.1</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Java Runtime:</span>
              <span className="font-semibold text-slate-800 dark:text-slate-200">Java 26</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Base URL:</span>
              <span className="font-mono text-indigo-600 dark:text-indigo-400">{API_BASE_URL}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Auth Mechanism:</span>
              <span className="font-medium text-slate-800 dark:text-slate-200">Stateless JWT + BCrypt (12 rounds)</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Database className="w-4 h-4 text-indigo-600" />
                <CardTitle className="text-sm">Data Persistence</CardTitle>
              </div>
              <Badge variant="secondary">Verified</Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <div className="flex justify-between">
              <span className="text-slate-500">Engine:</span>
              <span className="font-semibold text-slate-800 dark:text-slate-200">PostgreSQL 16</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Schema Version:</span>
              <span className="font-mono text-slate-800 dark:text-slate-200">001_initial_schema</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Tables:</span>
              <span className="font-medium text-slate-800 dark:text-slate-200">17 Normalized Tables</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">JPA Hibernate:</span>
              <span className="font-mono text-slate-800 dark:text-slate-200">ddl-auto: validate</span>
            </div>
          </CardContent>
        </Card>

        <Card className="md:col-span-2">
          <CardHeader>
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-emerald-600" />
              <CardTitle className="text-sm">Security & Access Control Governance</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-2 text-xs text-slate-600 dark:text-slate-400">
            <p>
              • Role-Based Access Control enforced at both Spring Security method boundaries (<code className="bg-slate-100 dark:bg-slate-800 px-1 py-0.5 rounded">@PreAuthorize</code>) and frontend route guards.
            </p>
            <p>
              • All customer and agent data isolated with row-level ownership assertions.
            </p>
            <p>
              • Zero client-side persistence of plain-text passwords or signing secrets.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
