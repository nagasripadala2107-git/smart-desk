'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { CustomerProfileResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { CustomerSummary } from '@/components/dashboard/CustomerSummary';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { User, Mail, Building, Hash, Phone } from 'lucide-react';

export default function CustomerProfilePage() {
  const [profile, setProfile] = useState<CustomerProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProfile = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.get<CustomerProfileResponse>('/customer/profile');
      setProfile(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load profile');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <PageHeader
        title="Customer Profile & Organization"
        description="View your company account details and active support tier."
      />

      {isLoading ? (
        <LoadingSkeleton type="details" />
      ) : error || !profile ? (
        <ErrorState
          title="Profile unavailable"
          message={error || 'Failed to load profile details'}
          onRetry={fetchProfile}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Account Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4 text-sm">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Full Name</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <User className="w-4 h-4 text-slate-400" />
                      {profile.firstName} {profile.lastName}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Email Address</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <Mail className="w-4 h-4 text-slate-400" />
                      {profile.email}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Organization</span>
                    <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <Building className="w-4 h-4 text-slate-400" />
                      {profile.companyName || 'Individual'}
                    </p>
                  </div>

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Customer Code</span>
                    <p className="font-mono font-bold text-indigo-600 dark:text-indigo-400 flex items-center gap-2">
                      <Hash className="w-4 h-4 text-slate-400" />
                      {profile.customerCode}
                    </p>
                  </div>

                  {profile.phone && (
                    <div className="space-y-1">
                      <span className="text-xs text-slate-500 font-medium">Phone</span>
                      <p className="font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                        <Phone className="w-4 h-4 text-slate-400" />
                        {profile.phone}
                      </p>
                    </div>
                  )}

                  <div className="space-y-1">
                    <span className="text-xs text-slate-500 font-medium">Support Plan</span>
                    <div className="pt-0.5">
                      <Badge variant="secondary">{profile.plan} Tier</Badge>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <CustomerSummary profile={profile} />
          </div>
        </div>
      )}
    </div>
  );
}
