# SmartDesk — Next.js Enterprise Frontend

SmartDesk is an AI-powered customer support and intelligent ticket routing platform. This directory contains the modern, responsive Next.js web application providing dedicated portals for Customers, Support Agents, and System Administrators.

---

## 1. Technology & Directory Structure

- **Framework**: Next.js 16 (App Router) + React 19 + TypeScript
- **Styling**: Tailwind CSS + shadcn/ui design tokens
- **Data Visualization & Icons**: Recharts, Lucide React
- **Architecture**: Zero mock persistence; connects directly to the Java Spring Boot REST API.

### Directory Layout
```text
frontend/
├── app/               # Next.js App Router (customer, agent, admin, auth pages)
├── components/        # Reusable UI components (buttons, badges, forms, charts)
├── hooks/             # Custom React hooks (auth, ticket queries)
├── lib/               # API client, constants, token storage utilities
├── types/             # Shared TypeScript domain interfaces
├── public/            # Static assets and icons
├── tests/             # Node native test runner test suite
└── Dockerfile         # Multi-stage production build (Node 26 Alpine)
```

---

## 2. Running the Frontend

### A. In Docker Compose (Recommended)
The frontend runs as part of the full containerized stack:
```bash
# From repository root
docker compose up -d frontend
```
- Available at [http://localhost:3000](http://localhost:3000)
- Health check verified automatically using native Node HTTP probe.

### B. Running Locally (Development)
```bash
# Install dependencies
npm install

# Start local Next.js development server
npm run dev
```
Runs at `http://localhost:3000` with hot module reloading (Turbopack).

---

## 3. Environment Configuration

Copy `.env.example` to `.env.local` for local execution:
```bash
cp .env.example .env.local
```

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api/v1` | Browser-accessible Spring Boot REST API endpoint |

> [!NOTE]
> The browser executes client components on the user's host machine. Therefore, `NEXT_PUBLIC_API_URL` must point to `http://localhost:8080/api/v1`, not the Docker internal DNS name.

---

## 4. Testing & Code Quality

```bash
# Run unit tests
npm test

# Run ESLint validation
npm run lint

# Build production standalone bundle
npm run build
```

---

## 5. Backend Dependency & Integration

The frontend connects directly to the Java Spring Boot backend at `http://localhost:8080/api/v1`:
- **Authentication**: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `GET /api/v1/auth/me`
- **Customer Portal**: `GET /api/v1/customer/profile`, `GET /api/v1/customer/tickets`
- **Agent Workspace**: `GET /api/v1/agent/queue`, `GET /api/v1/agent/tickets`, `GET /api/v1/agent/profile`
- **Tickets & Messages**: `POST /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `PATCH /api/v1/tickets/{id}`, `GET/POST /api/v1/tickets/{id}/messages`
- **Escalation**: `POST /api/v1/tickets/{id}/escalate`
- **Analytics & Master Data**: `GET /api/v1/analytics/overview`, `GET /api/v1/teams`, `GET /api/v1/categories`

---

## 5. Project Architecture

```
frontend/
├── app/
│   ├── layout.tsx                # Root layout with AuthProvider & Google Fonts
│   ├── page.tsx                  # SaaS Landing page + Ticket Lifecycle flow
│   ├── login/page.tsx            # Login with role-based redirection
│   ├── register/page.tsx         # Customer registration (strictly CUSTOMER role)
│   ├── forgot-password/page.tsx  # Password reset flow
│   ├── customer/                 # Customer Support Portal
│   │   ├── dashboard/page.tsx    # Customer KPIs & recent tickets
│   │   ├── tickets/page.tsx      # Filterable & searchable ticket list
│   │   ├── tickets/new/page.tsx  # Ticket creation form
│   │   ├── tickets/[id]/page.tsx # Ticket thread, timeline, and actions
│   │   └── profile/page.tsx      # Customer account & tier view
│   ├── agent/                    # Agent Workspace
│   │   ├── dashboard/page.tsx    # Agent queue & team queue
│   │   ├── tickets/page.tsx      # Assigned ticket queue
│   │   ├── tickets/[id]/page.tsx # Ticket triage, status/priority controls, escalate
│   │   ├── customers/[id]/page.tsx # Customer 360 view
│   │   ├── escalations/page.tsx  # Multi-tier escalation management
│   │   └── profile/page.tsx      # Agent skills & concurrency profile
│   └── admin/                    # Administrator Operations
│       ├── dashboard/page.tsx    # Operational KPIs + Recharts visualizations
│       ├── tickets/page.tsx      # Global ticket repository
│       ├── teams/page.tsx        # Active support teams
│       ├── categories/page.tsx   # System taxonomy categories
│       ├── customers/page.tsx    # Customer directory (Rule 16 placeholder)
│       ├── agents/page.tsx       # Agent directory (Rule 16 placeholder)
│       ├── routing-rules/page.tsx # AI/Deterministic routing (Phase 5 placeholder)
│       ├── escalation-rules/page.tsx # DAG Escalation policies (Phase 7 placeholder)
│       └── settings/page.tsx     # System infrastructure settings
├── components/
│   ├── ui/                       # Reusable primitives (Button, Card, Input, Select, Modal, etc.)
│   ├── common/                   # Shared UI (StatCard, StatusBadge, PriorityBadge, EmptyState, etc.)
│   ├── layout/                   # Shell components (Navbar, Sidebar, PageHeader, DashboardShell)
│   ├── tickets/                  # Ticket components (Table, Card, Timeline, MessageThread, Composer)
│   ├── dashboard/                # Dashboard widgets (LifecycleDiagram, Customer/Agent summaries)
│   └── analytics/                # Recharts charts (CategoryPieChart, PriorityBarChart, KpiCards)
├── lib/
│   ├── api.ts                    # Centralized API fetcher with Bearer JWT & error interception
│   ├── auth.ts                   # Token session management (passwords never stored)
│   ├── constants.ts              # Status mappings, theme tokens, routes
│   ├── utils.ts                  # Class merge (cn) and date formatting helpers
│   └── validations.ts            # Zod validation schemas
├── hooks/
│   └── useAuth.tsx               # AuthContext with login, register, logout, and role routing
├── types/
│   └── index.ts                  # Strict TypeScript interfaces matching Spring Boot DTOs
└── tests/
    └── frontend.test.mjs         # Test suite for validation schemas, error handling, and routing
```

---

## 6. Authentication & Role-Based Navigation

- **Roles Supported**: `CUSTOMER`, `AGENT`, `ADMIN`.
- **Navigation Redirection**:
  - `CUSTOMER` → `/customer/dashboard`
  - `AGENT` → `/agent/dashboard`
  - `ADMIN` → `/admin/dashboard`
- **Security Compliance**:
  - Passwords are never stored in browser storage.
  - JWT tokens are transmitted via standard HTTP `Authorization: Bearer <token>` headers.
  - Public registration is strictly restricted to `CUSTOMER` accounts.
  - Automatic session expiration detection with redirection on HTTP 401.
