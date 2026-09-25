'use client';

import React from 'react';
import { PageHeader } from '@/components/layout/PageHeader';
import { NotImplementedState } from '@/components/common/NotImplementedState';

export default function AdminRoutingRulesPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Automated Routing Rules"
        description="Rule engine for conditional matching, category-based routing, and skill-based assignment."
      />

      <NotImplementedState
        featureName="Deterministic & AI Routing Rule Engine"
        plannedPhase="Phase 5 (AI Service & Rule Engine)"
        description="Dynamic rule builder and keyword/sentiment conditional dispatching will integrate with the Python FastAPI classification microservice in Phase 5. Phase 3 provides deterministic category-to-team matching via database seed rules."
      />
    </div>
  );
}
