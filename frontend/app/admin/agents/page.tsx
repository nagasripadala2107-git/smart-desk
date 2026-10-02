'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
import { AgentPerformanceResponse, TeamResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import {
  Users,
  Headphones,
  CheckCircle2,
  Clock,
  Search,
  Filter,
  Shield,
  Layers,
  Sparkles,
  Zap,
} from 'lucide-react';

interface AgentWorkforceMember {
  id: string;
  name: string;
  email: string;
  employeeCode: string;
  team: string;
  tier: string;
  skills: string[];
  activeTickets: number;
  resolvedTickets: number;
  avgResolutionTime: string;
  status: 'ONLINE' | 'IN_CALL' | 'AWAY' | 'OFFLINE';
}

const DEFAULT_AGENTS: AgentWorkforceMember[] = [
  {
    id: 'agent-1',
    name: 'Bob Technician',
    email: 'agent.tech@smartdesk.local',
    employeeCode: 'EMP-TEC-102',
    team: 'Technical Support',
    tier: 'Tier 2 Technical Specialist',
    skills: ['Spring Boot', 'PostgreSQL', 'Docker', 'REST APIs', 'Cloud DB'],
    activeTickets: 3,
    resolvedTickets: 42,
    avgResolutionTime: '45 mins',
    status: 'ONLINE',
  },
  {
    id: 'agent-2',
    name: 'Sarah Chen',
    email: 'sarah.support@smartdesk.local',
    employeeCode: 'EMP-GEN-105',
    team: 'General Support',
    tier: 'Tier 1 Frontline Triage',
    skills: ['Customer Relations', 'Initial Triage', 'SLA Monitoring', 'Zendesk Migration'],
    activeTickets: 4,
    resolvedTickets: 89,
    avgResolutionTime: '18 mins',
    status: 'ONLINE',
  },
  {
    id: 'agent-3',
    name: 'Elena Rostova',
    email: 'elena.eng@smartdesk.local',
    employeeCode: 'EMP-SNR-201',
    team: 'Senior Support',
    tier: 'Tier 3 Core Infrastructure Lead',
    skills: ['Kubernetes', 'FastAPI', 'High-Availability', 'Disaster Recovery', 'Security'],
    activeTickets: 2,
    resolvedTickets: 31,
    avgResolutionTime: '1.8 hours',
    status: 'IN_CALL',
  },
  {
    id: 'agent-4',
    name: 'Marcus Vance',
    email: 'marcus.mgmt@smartdesk.local',
    employeeCode: 'EMP-MGT-301',
    team: 'Management',
    tier: 'Tier 4 Incident Commander',
    skills: ['Executive Escalation', 'Major Incident Response', 'Legal Compliance', 'Audit'],
    activeTickets: 1,
    resolvedTickets: 19,
    avgResolutionTime: '30 mins',
    status: 'ONLINE',
  },
];

export default function AdminAgentsPage() {
  const [agents, setAgents] = useState<AgentWorkforceMember[]>(DEFAULT_AGENTS);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTeam, setSelectedTeam] = useState<string>('ALL');

  useEffect(() => {
    const fetchWorkforce = async () => {
      setIsLoading(true);
      try {
        const perfData = await api.get<AgentPerformanceResponse[]>('/analytics/agent-performance').catch(() => []);
        if (perfData && perfData.length > 0) {
          // Merge API data with active workforce profiles
          const merged = DEFAULT_AGENTS.map((item) => {
            const apiMatch = perfData.find((p) => p.employeeCode === item.employeeCode || p.agentName.includes(item.name.split(' ')[0]));
            if (apiMatch) {
              return {
                ...item,
                name: apiMatch.agentName || item.name,
                team: apiMatch.teamName || item.team,
                activeTickets: apiMatch.openTickets || item.activeTickets,
                resolvedTickets: apiMatch.resolvedTickets || item.resolvedTickets,
                avgResolutionTime: apiMatch.avgResolutionMinutes ? `${Math.round(apiMatch.avgResolutionMinutes)} mins` : item.avgResolutionTime,
              };
            }
            return item;
          });
          setAgents(merged);
        }
      } catch (e) {
        console.error('Failed to load live agent metrics', e);
      } finally {
        setIsLoading(false);
      }
    };

    fetchWorkforce();
  }, []);

  const filteredAgents = agents.filter((agent) => {
    const matchesSearch =
      agent.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      agent.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      agent.employeeCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      agent.skills.some((s) => s.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesTeam = selectedTeam === 'ALL' || agent.team === selectedTeam;
    return matchesSearch && matchesTeam;
  });

  const getStatusBadge = (status: AgentWorkforceMember['status']) => {
    switch (status) {
      case 'ONLINE':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Online
          </span>
        );
      case 'IN_CALL':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
            <span className="w-1.5 h-1.5 rounded-full bg-indigo-500" />
            Active Session
          </span>
        );
      case 'AWAY':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
            Away
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
            Offline
          </span>
        );
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <PageHeader
        title="Agent Workforce & Skills Matrix"
        description="Comprehensive support representative directory, tier hierarchy, real-time queue workloads, and performance telemetry."
      />

      {/* Workforce KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="border-indigo-100 dark:border-indigo-950/80 bg-gradient-to-br from-indigo-50/40 via-white to-white dark:from-indigo-950/20 dark:via-slate-900 dark:to-slate-900">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Total Workforce</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">{agents.length}</h3>
              <p className="text-[11px] text-emerald-600 font-medium mt-0.5">100% capacity deployed</p>
            </div>
            <div className="p-3 rounded-xl bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
              <Users className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-emerald-100 dark:border-emerald-950/80 bg-gradient-to-br from-emerald-50/40 via-white to-white dark:from-emerald-950/20 dark:via-slate-900 dark:to-slate-900">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Active On-Duty</p>
              <h3 className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {agents.filter((a) => a.status === 'ONLINE' || a.status === 'IN_CALL').length}
              </h3>
              <p className="text-[11px] text-slate-500 mt-0.5">Ready for instant assignment</p>
            </div>
            <div className="p-3 rounded-xl bg-emerald-100 dark:bg-emerald-900/60 text-emerald-600 dark:text-emerald-400">
              <Headphones className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-blue-100 dark:border-blue-950/80 bg-gradient-to-br from-blue-50/40 via-white to-white dark:from-blue-950/20 dark:via-slate-900 dark:to-slate-900">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Active Ticket Queue</p>
              <h3 className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
                {agents.reduce((acc, curr) => acc + curr.activeTickets, 0)}
              </h3>
              <p className="text-[11px] text-slate-500 mt-0.5">Distributed across tiers</p>
            </div>
            <div className="p-3 rounded-xl bg-blue-100 dark:bg-blue-900/60 text-blue-600 dark:text-blue-400">
              <Layers className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-purple-100 dark:border-purple-950/80 bg-gradient-to-br from-purple-50/40 via-white to-white dark:from-purple-950/20 dark:via-slate-900 dark:to-slate-900">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Resolved All-Time</p>
              <h3 className="text-2xl font-bold text-purple-600 dark:text-purple-400 mt-1">
                {agents.reduce((acc, curr) => acc + curr.resolvedTickets, 0)}
              </h3>
              <p className="text-[11px] text-emerald-600 font-medium mt-0.5">High SLA compliance</p>
            </div>
            <div className="p-3 rounded-xl bg-purple-100 dark:bg-purple-900/60 text-purple-600 dark:text-purple-400">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card>
        <CardContent className="p-4 sm:p-5 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by agent name, skill, code..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 outline-none"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
            <span className="text-xs text-slate-400 flex items-center gap-1 shrink-0">
              <Filter className="w-3.5 h-3.5" /> Team:
            </span>
            {['ALL', 'Technical Support', 'General Support', 'Senior Support', 'Management'].map((team) => (
              <button
                key={team}
                onClick={() => setSelectedTeam(team)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                  selectedTeam === team
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                {team}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Agents Roster Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filteredAgents.map((agent) => (
          <Card key={agent.id} className="hover:border-indigo-200 dark:hover:border-indigo-800 transition-all shadow-xs">
            <CardHeader className="pb-3 flex flex-row items-start justify-between">
              <div className="flex items-start gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-100 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 font-bold text-sm border border-indigo-200 dark:border-indigo-800">
                  {agent.name.split(' ').map((n) => n[0]).join('')}
                </div>
                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{agent.name}</h4>
                    <span className="font-mono text-[10px] text-slate-500 bg-slate-100 dark:bg-slate-800 px-1.5 py-0.5 rounded">
                      {agent.employeeCode}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{agent.email}</p>
                  <p className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 mt-1 flex items-center gap-1">
                    <Shield className="w-3.5 h-3.5" />
                    {agent.tier}
                  </p>
                </div>
              </div>
              <div>{getStatusBadge(agent.status)}</div>
            </CardHeader>

            <CardContent className="space-y-3 pt-0">
              <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-100 dark:border-slate-800/80 grid grid-cols-3 gap-2 text-center text-xs">
                <div>
                  <span className="text-[10px] text-slate-400 uppercase font-medium">Active Queue</span>
                  <p className="font-bold text-slate-900 dark:text-slate-100 mt-0.5">{agent.activeTickets}</p>
                </div>
                <div className="border-x border-slate-200 dark:border-slate-800">
                  <span className="text-[10px] text-slate-400 uppercase font-medium">Resolved</span>
                  <p className="font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">{agent.resolvedTickets}</p>
                </div>
                <div>
                  <span className="text-[10px] text-slate-400 uppercase font-medium">Avg SLA</span>
                  <p className="font-bold text-indigo-600 dark:text-indigo-400 mt-0.5">{agent.avgResolutionTime}</p>
                </div>
              </div>

              <div>
                <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                  Verified Skills Matrix
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {agent.skills.map((skill) => (
                    <span
                      key={skill}
                      className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-[11px] font-medium text-slate-700 dark:text-slate-300"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
