'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { ArrowLeft, CheckCircle2 } from 'lucide-react';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    setSubmitted(true);
  };

  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-slate-50 dark:bg-slate-950">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Link
          href="/login"
          className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 mb-6 mx-auto block w-fit"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to sign in
        </Link>

        <h2 className="text-center text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          Reset your password
        </h2>
        <p className="mt-1 text-center text-xs sm:text-sm text-slate-500 dark:text-slate-400">
          Enter your registered email and we will send password reset instructions
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <Card>
          <CardContent className="p-6 sm:p-8">
            {submitted ? (
              <div className="text-center py-4 space-y-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 mx-auto">
                  <CheckCircle2 className="w-6 h-6" />
                </div>
                <h4 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                  Instructions Sent
                </h4>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  If an account exists with <span className="font-semibold">{email}</span>, password reset details have been dispatched.
                </p>
                <Link href="/login" className="block pt-2">
                  <Button variant="outline" size="sm" className="w-full">
                    Return to sign in
                  </Button>
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <Input
                  label="Work Email"
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@company.com"
                  required
                />
                <Button type="submit" className="w-full mt-2">
                  Send Reset Instructions
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
