'use client';

import React from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { NotImplementedState } from '@/components/common/NotImplementedState';

export default function AdminCustomersPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Customer Management Directory"
        description="Global customer directory, subscription tiers, and organization accounts."
      />

      <NotImplementedState
        featureName="Customer Management CRUD API"
        plannedPhase="Phase 5 & 6"
        description="Administrative customer lifecycle endpoints (account provisioning, plan adjustments, and customer suspension) are pending backend API exposure. Phase 3 provides /api/v1/customer/profile for the authenticated user context."
      />
    </div>
  );
}
