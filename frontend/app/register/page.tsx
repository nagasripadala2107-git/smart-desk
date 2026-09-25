'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/hooks/useAuth';
import { registerSchema } from '@/lib/validations';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { AlertCircle, ArrowLeft, ShieldCheck } from 'lucide-react';

export default function RegisterPage() {
  const { register } = useAuth();

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    companyName: '',
    phone: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    setServerError(null);

    const validation = registerSchema.safeParse(formData);
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
      await register({
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        password: formData.password,
        companyName: formData.companyName || undefined,
        phone: formData.phone || undefined,
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Registration failed. Please try again.';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

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
          Create Customer Account
        </h2>
        <p className="mt-1 text-center text-xs sm:text-sm text-slate-500 dark:text-slate-400">
          Get support assistance and submit tickets with ease
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-lg px-4 sm:px-0">
        <Card>
          <CardContent className="p-6 sm:p-8">
            <div className="mb-4 p-2.5 rounded-lg bg-indigo-50/60 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-900/60 text-indigo-700 dark:text-indigo-300 text-xs flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 shrink-0 text-indigo-600" />
              <span>
                Public registration provisions <strong>Customer</strong> access. Support Agents and
                Administrators are provisioned internally.
              </span>
            </div>

            {serverError && (
              <div className="mb-4 p-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{serverError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  label="First Name"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  placeholder="John"
                  error={errors.firstName}
                  required
                />
                <Input
                  label="Last Name"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  placeholder="Doe"
                  error={errors.lastName}
                  required
                />
              </div>

              <Input
                label="Work Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="john@example.com"
                error={errors.email}
                required
              />

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  label="Company / Org Name (Optional)"
                  name="companyName"
                  value={formData.companyName}
                  onChange={handleChange}
                  placeholder="Acme Corp"
                  error={errors.companyName}
                />
                <Input
                  label="Phone Number (Optional)"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="+1 (555) 000-0000"
                  error={errors.phone}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  label="Password (min 8 chars)"
                  name="password"
                  type="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  error={errors.password}
                  required
                />
                <Input
                  label="Confirm Password"
                  name="confirmPassword"
                  type="password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="••••••••"
                  error={errors.confirmPassword}
                  required
                />
              </div>

              <Button type="submit" className="w-full mt-2" isLoading={isLoading}>
                Create Customer Account
              </Button>
            </form>

            <div className="mt-6 border-t border-slate-100 dark:border-slate-800 pt-4 text-center">
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Already have an account?{' '}
                <Link
                  href="/login"
                  className="font-medium text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 hover:underline"
                >
                  Sign in instead
                </Link>
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
