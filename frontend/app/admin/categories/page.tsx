'use client';

import React, { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { CategoryResponse } from '@/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { Tag } from 'lucide-react';

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCategories = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.get<CategoryResponse[]>('/categories');
      setCategories(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load categories');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Ticket Categories"
        description="Active taxonomy categories used by customers, agents, and routing engines."
      />

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
          <LoadingSkeleton rows={2} />
        </div>
      ) : error ? (
        <ErrorState message={error} onRetry={fetchCategories} />
      ) : categories.length === 0 ? (
        <EmptyState
          title="No categories registered"
          description="No support taxonomy categories found."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {categories.map((cat) => (
            <Card key={cat.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
                      <Tag className="w-4 h-4" />
                    </div>
                    <CardTitle className="text-sm font-semibold">{cat.name}</CardTitle>
                  </div>
                  <Badge variant={cat.isActive ? 'success' : 'default'}>
                    {cat.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                {cat.description || 'System taxonomy category for incoming issue routing.'}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
