'use client';

import React from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from 'recharts';
import { PriorityStatsResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

interface PriorityBarChartProps {
  data: PriorityStatsResponse[];
}

const PRIORITY_COLOR_MAP: Record<string, string> = {
  LOW: '#64748b',
  MEDIUM: '#0ea5e9',
  HIGH: '#f59e0b',
  URGENT: '#f43f5e',
};

export function PriorityBarChart({ data }: PriorityBarChartProps) {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Tickets by Priority</CardTitle>
        </CardHeader>
        <CardContent className="h-64 flex items-center justify-center text-xs text-slate-400">
          No priority data available
        </CardContent>
      </Card>
    );
  }

  const chartData = data.map((item) => ({
    priority: item.priority,
    count: Number(item.ticketCount),
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Tickets by Priority</CardTitle>
      </CardHeader>
      <CardContent className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
            <XAxis
              dataKey="priority"
              tick={{ fontSize: 11, fill: '#64748b' }}
              axisLine={{ stroke: '#e2e8f0' }}
              tickLine={false}
            />
            <YAxis
              tick={{ fontSize: 11, fill: '#64748b' }}
              axisLine={{ stroke: '#e2e8f0' }}
              tickLine={false}
              allowDecimals={false}
            />
            <Tooltip
              formatter={(value) => [`${value} tickets`, 'Count']}
              contentStyle={{
                backgroundColor: '#0f172a',
                borderColor: '#1e293b',
                color: '#fff',
                borderRadius: '8px',
                fontSize: '12px',
              }}
            />
            <Bar dataKey="count" radius={[6, 6, 0, 0]}>
              {chartData.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={PRIORITY_COLOR_MAP[entry.priority] || '#6366f1'}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
