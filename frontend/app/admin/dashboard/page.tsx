'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import {
  AnalyticsOverviewResponse,
  CategoryStatsResponse,
  PriorityStatsResponse,
  StatusStatsResponse,
  TicketVolumeResponse,
  TeamWorkloadResponse,
  AgentPerformanceResponse,
  EscalationStatsResponse,
  SlaAnalyticsResponse,
} from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { OverviewKpiCards } from '@/components/analytics/OverviewKpiCards';
import { TicketVolumeChart } from '@/components/analytics/TicketVolumeChart';
import { CategoryPieChart } from '@/components/analytics/CategoryPieChart';
import { PriorityBarChart } from '@/components/analytics/PriorityBarChart';
import { StatusDonutChart } from '@/components/analytics/StatusDonutChart';
import { TeamWorkloadTable } from '@/components/analytics/TeamWorkloadTable';
import { AgentPerformanceTable } from '@/components/analytics/AgentPerformanceTable';
import { EscalationOverviewCard } from '@/components/analytics/EscalationOverviewCard';
import { SlaComplianceCard } from '@/components/analytics/SlaComplianceCard';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { RefreshCw, Database } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export default function AdminDashboardPage() {
  const [overview, setOverview] = useState<AnalyticsOverviewResponse | null>(null);
  const [ticketVolume, setTicketVolume] = useState<TicketVolumeResponse[]>([]);
  const [categoryStats, setCategoryStats] = useState<CategoryStatsResponse[]>([]);
  const [priorityStats, setPriorityStats] = useState<PriorityStatsResponse[]>([]);
  const [statusStats, setStatusStats] = useState<StatusStatsResponse[]>([]);
  const [teamWorkload, setTeamWorkload] = useState<TeamWorkloadResponse[]>([]);
  const [agentPerformance, setAgentPerformance] = useState<AgentPerformanceResponse[]>([]);
  const [escalations, setEscalations] = useState<EscalationStatsResponse | null>(null);
  const [sla, setSla] = useState<SlaAnalyticsResponse | null>(null);

  const [days, setDays] = useState<number>(30);
  const [isLoading, setIsLoading] = useState(true);
  const [isVolumeLoading, setIsVolumeLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [
        overviewData,
        volumeData,
        catData,
        priData,
        statusData,
        teamData,
        agentData,
        escData,
        slaData,
      ] = await Promise.all([
        api.get<AnalyticsOverviewResponse>('/analytics/overview'),
        api.get<TicketVolumeResponse[]>(`/analytics/tickets-over-time?days=${days}`),
        api.get<CategoryStatsResponse[]>('/analytics/tickets-by-category'),
        api.get<PriorityStatsResponse[]>('/analytics/tickets-by-priority'),
        api.get<StatusStatsResponse[]>('/analytics/tickets-by-status'),
        api.get<TeamWorkloadResponse[]>('/analytics/team-performance'),
        api.get<AgentPerformanceResponse[]>('/analytics/agent-performance'),
        api.get<EscalationStatsResponse>('/analytics/escalations'),
        api.get<SlaAnalyticsResponse>('/analytics/sla'),
      ]);

      setOverview(overviewData);
      setTicketVolume(volumeData);
      setCategoryStats(catData);
      setPriorityStats(priData);
      setStatusStats(statusData);
      setTeamWorkload(teamData);
      setAgentPerformance(agentData);
      setEscalations(escData);
      setSla(slaData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load support analytics');
    } finally {
      setIsLoading(false);
    }
  }, [days]);

  const handleDaysChange = async (newDays: number) => {
    setDays(newDays);
    setIsVolumeLoading(true);
    try {
      const volumeData = await api.get<TicketVolumeResponse[]>(
        `/analytics/tickets-over-time?days=${newDays}`
      );
      setTicketVolume(volumeData);
    } catch (err: unknown) {
      console.error('Failed to update ticket volume:', err);
    } finally {
      setIsVolumeLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Admin Operations & Analytics"
          description="Operational KPIs, ticket volume trends, team workloads, and SLA compliance metrics."
        />
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
        </div>
        <LoadingSkeleton type="card" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <LoadingSkeleton rows={5} />
          <LoadingSkeleton rows={5} />
          <LoadingSkeleton rows={5} />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <LoadingSkeleton rows={4} />
          <LoadingSkeleton rows={4} />
        </div>
      </div>
    );
  }

  if (error || !overview) {
    return (
      <div className="py-12">
        <ErrorState
          title="Analytics Unavailable"
          message={error || 'Unable to retrieve operational analytics'}
          onRetry={fetchDashboardData}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <PageHeader
          title="Admin Operations & Analytics"
          description="Operational KPIs, ticket volume trends, team workloads, and SLA compliance metrics."
        />
        <div className="flex items-center gap-2 shrink-0">
          <span className="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
            <Database className="w-3.5 h-3.5 text-indigo-500" />
            Operational data from PostgreSQL
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchDashboardData}
            className="flex items-center gap-1.5 text-xs"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Refresh
          </Button>
        </div>
      </div>

      {/* 1. Overview KPI Cards (6 metrics + secondary resolution time) */}
      <OverviewKpiCards overview={overview} />

      {/* 2. Ticket Volume Over Time (Interactive with 7D, 30D, 90D, All) */}
      <TicketVolumeChart
        data={ticketVolume}
        days={days}
        onDaysChange={handleDaysChange}
        isLoading={isVolumeLoading}
      />

      {/* 3. Category, Priority, and Status Distributions */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <CategoryPieChart data={categoryStats} />
        <PriorityBarChart data={priorityStats} />
        <StatusDonutChart data={statusStats} />
      </div>

      {/* 4. Team Workload & Agent Operational Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TeamWorkloadTable data={teamWorkload} />
        <AgentPerformanceTable data={agentPerformance} />
      </div>

      {/* 5. Escalation Overview & SLA Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {escalations && <EscalationOverviewCard data={escalations} />}
        {sla && <SlaComplianceCard data={sla} />}
      </div>
    </div>
  );
}
