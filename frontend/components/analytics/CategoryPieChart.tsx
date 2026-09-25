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
import { CategoryStatsResponse } from '@/types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

interface CategoryPieChartProps {
  data: CategoryStatsResponse[];
}

const COLORS = ['#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#64748b'];

export function CategoryPieChart({ data }: CategoryPieChartProps) {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Tickets by Category</CardTitle>
        </CardHeader>
        <CardContent className="h-64 flex items-center justify-center text-xs text-slate-400">
          No category data available
        </CardContent>
      </Card>
    );
  }

  const chartData = data.map((item) => ({
    name: item.categoryName,
    value: Number(item.ticketCount),
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Tickets by Category</CardTitle>
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
              paddingAngle={4}
              dataKey="value"
            >
              {chartData.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
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
