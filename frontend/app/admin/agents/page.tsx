'use client';

import React from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { NotImplementedState } from '@/components/common/NotImplementedState';

export default function AdminAgentsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Agent Workforce Directory"
        description="Support representative directory, skills matrix, and team assignment controls."
      />

      <NotImplementedState
        featureName="Agent Directory Management API"
        plannedPhase="Phase 5 & 6"
        description="Administrative agent creation, team reassignment, and skill matrix updates are pending dedicated administrative CRUD endpoint exposure. Phase 3 provides individual agent queue and self-profile endpoints."
      />
    </div>
  );
}
