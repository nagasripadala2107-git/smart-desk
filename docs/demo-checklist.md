# SmartDesk Production Demonstration Checklist & Presentation Guide

This guide provides a structured 10–15 minute live demonstration walkthrough of the SmartDesk platform for stakeholders, academic evaluators, or client presentations.

---

## Demonstration Overview

- **Target Duration**: 12–15 minutes
- **Roles Demonstrated**: Customer (`customer@example.com`), Support Agent (`agent@example.com`), Administrator (`admin@example.com`)
- **Key Themes**: Real-time triage, localized ML inference, automated routing, SLA compliance, resilient multi-tier architecture.

---

## Pre-Demo Preparation (5 Minutes Before)

- [ ] Ensure SmartDesk stack is running (`docker compose ps`).
- [ ] Open modern browser in private/incognito mode.
- [ ] Have test credentials ready:
  - Customer: `customer@example.com` / `password123`
  - Agent: `agent@example.com` / `password123`
  - Admin: `admin@example.com` / `password123`
- [ ] Open terminal window in background (optional, for viewing logs or Docker status).

---

## 15-Minute Demonstration Script

| Step | Time | Screen / Role | Action & Talking Points | Verification Check |
| :---: | :---: | :--- | :--- | :--- |
| **1** | 0:00 - 1:00 | **Landing Page** | Navigate to `https://<domain>/`. Introduce SmartDesk as an enterprise AI-powered service desk platform built with Spring Boot, Next.js, FastAPI, and PostgreSQL. Highlight the responsive dark/light UI and TLS security. | Hero section renders cleanly; HTTPS active. |
| **2** | 1:00 - 1:45 | **Customer Login** | Log in as `customer@example.com`. Explain JWT authentication with BCrypt hashing and 30 req/min rate-limiting to prevent credential stuffing. | Customer redirected to `/dashboard/customer`. |
| **3** | 1:45 - 3:00 | **Create Support Ticket** | Click **New Ticket**. Enter title: `VPN Connection Dropping Intermittently` and description: `I cannot connect to the corporate VPN from home. It disconnects every 5 minutes and blocks my critical deployment work! Please fix ASAP.` | Form validates inputs; submit button triggers POST. |
| **4** | 3:00 - 4:00 | **AI Classification & Routing** | Submit ticket. Explain that upon ingestion, Spring Boot synchronously queries the Python FastAPI AI service via Docker internal DNS. The TF-IDF + Logistic Regression model analyzes text semantics. | Ticket created; category assigned to `Network` or `Software`. |
| **5** | 4:00 - 4:45 | **Customer Ticket View** | View ticket detail as Customer. Demonstrate that the customer sees status `CREATED` or `ASSIGNED`, priority, and team routing. Point out that internal sentiment and duplicate metadata are strictly hidden from the customer. | Customer isolation verified; no internal metadata leaked. |
| **6** | 4:45 - 5:45 | **Agent Login & Queue** | Log out, log in as `agent@example.com`. Navigate to Agent Queue. Show how tickets are partitioned across teams and prioritized by SLA urgency. | Newly created ticket visible in agent queue. |
| **7** | 5:45 - 7:00 | **AI Sentiment & Tone** | Open the ticket in Agent workspace. Point out the AI Intelligence card: Sentiment: `NEGATIVE`, Tone: `FRUSTRATED` / `URGENT`. Explain how this empowers agents to adjust tone and prioritize critical client issues. | Sentiment badges render properly. |
| **8** | 7:00 - 8:00 | **Duplicate Detection** | Show the **Duplicate Candidates** section on the agent panel. Explain TF-IDF vectorization and cosine similarity matching (>0.75) against open tickets in the same category. | Candidate duplicate tickets displayed with similarity scores. |
| **9** | 8:00 - 9:00 | **Customer 360 Context** | Point out the Customer 360 side panel showing customer history: total tickets, open issues, SLA breach history, and satisfaction metrics. | Past customer interaction summary loads instantly. |
| **10** | 9:00 - 10:00 | **Agent Reply & Conversation** | Post an agent message: `Hello, we have identified a gateway load balancing issue and are re-routing your VPN tunnel. Please retry in 2 minutes.` | Real-time message thread updates; event audit logged. |
| **11** | 10:00 - 10:45 | **Escalation & Reassignment** | Show manual escalation workflow or team reassignment (e.g. transfer to Tier 2 Network Engineering). Explain automated SLA countdown timers. | Ticket timeline reflects assignment and event log. |
| **12** | 10:45 - 11:30 | **Resolve & Close Ticket** | Update ticket status to `RESOLVED` with resolution notes. Demonstrate ticket lifecycle transition. | Status badge changes to green `RESOLVED`. |
| **13** | 11:30 - 12:45 | **Admin Analytics Dashboard** | Log out, log in as `admin@example.com`. Navigate to `/dashboard/admin/analytics`. Showcase real-time metric cards: Total Tickets, Resolution Time, Category Distribution, SLA Compliance Rate, and Sentiment Trends. | Charts and aggregated analytics load smoothly. |
| **14** | 12:45 - 13:45 | **Architecture & Security Overview** | Present the system architecture: Nginx reverse proxy with TLS termination, Next.js frontend, Java Spring Boot business backend, Python FastAPI AI microservice, and PostgreSQL 17 database. Highlight that only Nginx exposes public ports. | Reference `docs/production-architecture.md`. |
| **15** | 13:45 - 15:00 | **Reliability & AI Fallback Q&A** | Explain fault tolerance: if the AI microservice is down, ticket creation completes seamlessly in fallback mode without dropping customer requests or showing stack traces. Conclude demonstration and open for questions. | Demonstrate engineering maturity and design trade-offs. |

---

## Key Talking Points & Architectural Highlights

1. **Enterprise Grade Separation**:
   - Backend business logic, transactions, and role enforcement are governed by Java 26 and Spring Boot 4.1.1.
   - Machine learning workloads are isolated in a specialized Python FastAPI service with dedicated resource limits.
2. **True Micro-Segmentation**:
   - PostgreSQL and the AI service are completely invisible to the public internet, accessible only over internal Docker bridge networks.
3. **Zero External AI Dependencies**:
   - All classification, sentiment, tone, and duplicate algorithms execute locally on-CPU without expensive, slow, or privacy-invasive third-party LLM APIs.
4. **Resilient Fail-Safe Ingestion**:
   - High availability design guarantees customer ticket intake never fails, even under total AI service outage.
