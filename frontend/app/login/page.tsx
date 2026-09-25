'use client';

import React, { useState, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import { loginSchema } from '@/lib/validations';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { AlertCircle, ArrowLeft, CheckCircle2 } from 'lucide-react';

function LoginForm() {
  const { login } = useAuth();
  const searchParams = useSearchParams();
  const expired = searchParams.get('expired');
  const registered = searchParams.get('registered');

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    setServerError(null);

    const validation = loginSchema.safeParse({ email, password });
    if (!validation.success) {
      const fieldErrors: Record<string, string> = {};
      validation.error.issues.forEach((err) => {
        if (err.path[0]) {
          fieldErrors[err.path[0].toString()] = err.message;
        }
      });
      setErrors(fieldErrors);
      return;
    }

    setIsLoading(true);
    try {
      await login({ email, password });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Invalid credentials. Please try again.';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6 sm:p-8">
        {expired && (
          <div className="mb-4 p-3 rounded-lg bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-amber-800 dark:text-amber-200 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>Your session expired. Please sign in again.</span>
          </div>
        )}

        {registered && (
          <div className="mb-4 p-3 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-200 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>Account created successfully! Please sign in.</span>
          </div>
        )}

        {serverError && (
          <div className="mb-4 p-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Work Email"
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@company.com"
            error={errors.email}
            autoComplete="email"
            required
          />

          <div className="space-y-1">
            <div className="flex items-center justify-between">
              <label htmlFor="password" className="text-xs font-medium text-slate-700 dark:text-slate-300">
                Password
              </label>
              <Link
                href="/forgot-password"
                className="text-xs text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 hover:underline"
              >
                Forgot password?
              </Link>
            </div>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className={`flex h-9 w-full rounded-lg border px-3 py-1.5 text-sm bg-white dark:bg-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
                errors.password ? 'border-rose-500' : 'border-slate-300 dark:border-slate-700'
              }`}
              autoComplete="current-password"
              required
            />
            {errors.password && <p className="text-xs text-rose-500">{errors.password}</p>}
          </div>

          <Button type="submit" className="w-full mt-2" isLoading={isLoading}>
            Sign In
          </Button>
        </form>

        <div className="mt-6 border-t border-slate-100 dark:border-slate-800 pt-4 text-center">
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Don&apos;t have an account?{' '}
            <Link
              href="/register"
              className="font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 hover:underline"
            >
              Create customer account
            </Link>
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-slate-50 dark:bg-slate-950">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 mb-6 mx-auto block w-fit"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to home
        </Link>

        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-600 text-white font-bold text-xl shadow-lg shadow-indigo-500/20">
            S
          </div>
        </div>

        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          Sign in to SmartDesk
        </h2>
        <p className="mt-1 text-center text-xs sm:text-sm text-slate-500 dark:text-slate-400">
          Enter your credentials to access your support workspace
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <Suspense fallback={<Card className="p-8 text-center text-xs text-slate-400">Loading form...</Card>}>
          <LoginForm />
        </Suspense>
      </div>
    </div>
  );
}
