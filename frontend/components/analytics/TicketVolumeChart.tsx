'use client';

import React from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { TicketVolumeResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Activity } from 'lucide-react';

interface TicketVolumeChartProps {
  data: TicketVolumeResponse[];
  days: number;
  onDaysChange: (days: number) => void;
  isLoading?: boolean;
}

export function TicketVolumeChart({
  data,
  days,
  onDaysChange,
  isLoading = false,
}: TicketVolumeChartProps) {
  const ranges = [
    { label: '7D', value: 7 },
    { label: '30D', value: 30 },
    { label: '90D', value: 90 },
    { label: 'All', value: 0 },
  ];

  const formatDateLabel = (dateStr: string) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[1]}/${parts[2]}`;
    }
    return dateStr;
  };

  return (
    <Card className="flex flex-col">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <div className="space-y-0.5">
          <div className="flex items-center gap-2">
            <Activity className="w-4 h-4 text-indigo-500" />
            <CardTitle className="text-base font-semibold">Ticket Volume Over Time</CardTitle>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Daily ticket volume (Operational data from PostgreSQL)
          </p>
        </div>
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-lg">
          {ranges.map((r) => (
            <button
              key={r.label}
              onClick={() => onDaysChange(r.value)}
              disabled={isLoading}
              className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
                days === r.value
                  ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
              }`}
            >
              {r.label}
            </button>
          ))}
        </div>
      </CardHeader>
      <CardContent className="h-72 pt-4">
        {data.length === 0 ? (
          <div className="h-full flex items-center justify-center text-xs text-slate-400">
            No ticket volume recorded in this time range
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="volumeGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" opacity={0.5} />
              <XAxis
                dataKey="date"
                tickFormatter={formatDateLabel}
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
                formatter={(val) => [`${val} tickets`, 'Volume']}
                labelFormatter={(label) => `Date: ${label}`}
                contentStyle={{
                  backgroundColor: '#0f172a',
                  borderColor: '#1e293b',
                  color: '#fff',
                  borderRadius: '8px',
                  fontSize: '12px',
                }}
              />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#6366f1"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#volumeGradient)"
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}
