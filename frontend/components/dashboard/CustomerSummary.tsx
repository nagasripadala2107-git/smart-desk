import React from 'react';
import { CustomerProfileResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Building2, Mail, Phone, Hash } from 'lucide-react';

interface CustomerSummaryProps {
  profile: CustomerProfileResponse;
}

export function CustomerSummary({ profile }: CustomerSummaryProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm">Customer Profile</CardTitle>
          <Badge variant="secondary">{profile.plan || 'Standard'} Plan</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 text-xs">
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Building2 className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="font-medium text-slate-900 dark:text-slate-100">{profile.companyName || 'Personal'}</span>
        </div>
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Mail className="w-4 h-4 text-slate-400 shrink-0" />
          <span>{profile.email}</span>
        </div>
        {profile.phone && (
          <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
            <Phone className="w-4 h-4 text-slate-400 shrink-0" />
            <span>{profile.phone}</span>
          </div>
        )}
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
          <Hash className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="font-mono">{profile.customerCode}</span>
        </div>
        <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex justify-between text-slate-500">
          <span>Total Tickets: <strong className="text-slate-900 dark:text-slate-100">{profile.totalTickets}</strong></span>
          <span>Active: <strong className="text-indigo-600 dark:text-indigo-400">{profile.activeTickets}</strong></span>
        </div>
      </CardContent>
    </Card>
  );
}
