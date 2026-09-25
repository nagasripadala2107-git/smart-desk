import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Navbar } from '@/components/layout/Navbar';
import { LifecycleDiagram } from '@/components/dashboard/LifecycleDiagram';
import {
  Sparkles,
  GitMerge,
  AlertOctagon,
  Users2,
  Clock4,
  BarChart3,
  ArrowRight,
  CheckCircle,
  ShieldCheck,
  Zap,
} from 'lucide-react';

export default function LandingPage() {
  const features = [
    {
      title: 'AI Ticket Classification',
      desc: 'Automatically classifies incoming requests by intent, urgency, and category for faster initial response.',
      icon: Sparkles,
      color: 'text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-950/60',
    },
    {
      title: 'Smart Team Routing',
      desc: 'Matches every issue with the optimal support tier and agent based on real-time skills and capacity.',
      icon: GitMerge,
      color: 'text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60',
    },
    {
      title: 'Escalation Tracking',
      desc: 'Multi-tier escalation boundaries ensure complex technical blockers are tracked with full audit history.',
      icon: AlertOctagon,
      color: 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/60',
    },
    {
      title: 'Customer 360',
      desc: 'Complete customer context, past interaction history, and organization health directly inside every ticket.',
      icon: Users2,
      color: 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60',
    },
    {
      title: 'SLA Monitoring',
      desc: 'Proactive alerts on SLA risk and response time thresholds keep team commitments transparent.',
      icon: Clock4,
      color: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60',
    },
    {
      title: 'Support Analytics',
      desc: 'Live KPI reporting on resolution time, category breakdowns, and agent workloads powered by Java analytics.',
      icon: BarChart3,
      color: 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60',
    },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-white dark:bg-slate-950">
      <Navbar />

      {/* Hero Section */}
      <section className="relative overflow-hidden pt-16 pb-20 sm:pt-24 sm:pb-28 border-b border-slate-100 dark:border-slate-800">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(45rem_50rem_at_top,theme(colors.indigo.100),white)] dark:bg-[radial-gradient(45rem_50rem_at_top,theme(colors.indigo.950),theme(colors.slate.950))] opacity-40" />

        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 mb-6">
            <Zap className="w-3.5 h-3.5" />
            Enterprise Support Platform
          </div>

          <h1 className="text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 dark:text-white max-w-4xl mx-auto leading-tight">
            AI-powered customer support that routes every issue to the right team.
          </h1>

          <p className="mt-6 text-base sm:text-lg text-slate-600 dark:text-slate-300 max-w-2xl mx-auto leading-relaxed">
            Eliminate triage bottlenecks with intelligent ticket classification, deterministic team
            routing, escalation tracking, and 360° customer history.
          </p>

          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link href="/register" className="w-full sm:w-auto">
              <Button size="lg" className="w-full sm:w-auto gap-2 text-sm shadow-md">
                Get Started
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
            <Link href="/login" className="w-full sm:w-auto">
              <Button variant="outline" size="lg" className="w-full sm:w-auto text-sm">
                Sign In
              </Button>
            </Link>
          </div>

          <div className="mt-12 flex flex-wrap items-center justify-center gap-6 text-xs text-slate-500 dark:text-slate-400">
            <div className="flex items-center gap-1.5">
              <CheckCircle className="w-4 h-4 text-emerald-500" />
              <span>Role-Based Access Control</span>
            </div>
            <div className="flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-indigo-500" />
              <span>Audit Logging & Security</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-amber-500" />
              <span>Zero-Lag Spring Boot Backend</span>
            </div>
          </div>
        </div>
      </section>

      {/* Ticket Lifecycle SaaS Flow Section */}
      <section className="py-16 bg-slate-50/70 dark:bg-slate-900/40 border-b border-slate-100 dark:border-slate-800">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-10">
            <h2 className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
              Deterministic Workflow
            </h2>
            <p className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white mt-1">
              The SmartDesk Ticket Lifecycle
            </p>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-2">
              From submission to resolution, every ticket passes through structured, auditable states.
            </p>
          </div>

          <LifecycleDiagram />
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-16 sm:py-24">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-14">
            <h2 className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
              Platform Capabilities
            </h2>
            <p className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-white mt-1">
              Engineered for Speed, Precision & Reliability
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feat, idx) => {
              const Icon = feat.icon;
              return (
                <div
                  key={idx}
                  className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-xs hover:shadow-md transition-shadow"
                >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center mb-4 ${feat.color}`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                    {feat.title}
                  </h3>
                  <p className="mt-2 text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
                    {feat.desc}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 py-8">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500 dark:text-slate-400">
          <p>© 2026 SmartDesk Platform. Enterprise Support Operations.</p>
          <div className="flex items-center gap-4">
            <Link href="/login" className="hover:text-slate-900 dark:hover:text-slate-200">
              Sign In
            </Link>
            <Link href="/register" className="hover:text-slate-900 dark:hover:text-slate-200">
              Customer Registration
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
