'use client';

import React from 'react';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
} from 'recharts';
import { StatusStatsResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

interface StatusDonutChartProps {
  data: StatusStatsResponse[];
}

const STATUS_COLOR_MAP: Record<string, string> = {
  OPEN: '#3b82f6',
  IN_PROGRESS: '#8b5cf6',
  PENDING_CUSTOMER: '#f59e0b',
  PENDING_INTERNAL: '#eab308',
  ESCALATED: '#f43f5e',
  RESOLVED: '#10b981',
  CLOSED: '#64748b',
};

export function StatusDonutChart({ data }: StatusDonutChartProps) {
  const chartData = data
    .filter((d) => d.ticketCount > 0)
    .map((item) => ({
      name: item.status.replace('_', ' '),
      value: Number(item.ticketCount),
      rawStatus: item.status,
      percentage: item.percentage,
    }));

  if (chartData.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Ticket Status Distribution</CardTitle>
        </CardHeader>
        <CardContent className="h-72 flex items-center justify-center text-xs text-slate-400">
          No tickets by status available
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Ticket Status Distribution</CardTitle>
      </CardHeader>
      <CardContent className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={55}
              outerRadius={80}
              paddingAngle={3}
              dataKey="value"
            >
              {chartData.map((entry) => (
                <Cell
                  key={`status-cell-${entry.rawStatus}`}
                  fill={STATUS_COLOR_MAP[entry.rawStatus] || '#6366f1'}
                />
              ))}
            </Pie>
            <Tooltip
              formatter={(value, name, props) => [
                `${value} tickets (${(props?.payload as { percentage?: number })?.percentage ?? 0}%)`,
                name,
              ]}
              contentStyle={{
                backgroundColor: '#0f172a',
                borderColor: '#1e293b',
                color: '#fff',
                borderRadius: '8px',
                fontSize: '12px',
              }}
            />
            <Legend
              wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }}
              iconType="circle"
            />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
