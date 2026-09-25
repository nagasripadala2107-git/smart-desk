# SmartDesk Production Deployment Runbook

This runbook provides complete, provider-neutral, step-by-step instructions for deploying and operating **SmartDesk** in a real production Linux server environment.

---

## Table of Contents
- [A. Prerequisites](#a-prerequisites)
- [B. Server Preparation](#b-server-preparation)
- [C. Docker Installation](#c-docker-installation)
- [D. Project Installation](#d-project-installation)
- [E. Environment Configuration](#e-environment-configuration)
- [F. Secret Generation](#f-secret-generation)
- [G. DNS Configuration](#g-dns-configuration)
- [H. TLS Certificate Configuration](#h-tls-certificate-configuration)
- [I. Production Image Build](#i-production-image-build)
- [J. Database Initialization](#j-database-initialization)
- [K. Production Startup](#k-production-startup)
- [L. Health Verification](#l-health-verification)
- [M. Login Verification](#m-login-verification)
- [N. Ticket Workflow Verification](#n-ticket-workflow-verification)
- [O. Backup Verification](#o-backup-verification)
- [P. Monitoring](#p-monitoring)
- [Q. Shutdown Procedure](#q-shutdown-procedure)
- [R. Restart Procedure](#r-restart-procedure)
- [S. Rollback Procedure](#s-rollback-procedure)
- [T. Disaster Recovery](#t-disaster-recovery)

---

## A. Prerequisites
- **Target OS**: Linux (Ubuntu 22.04 LTS, Ubuntu 24.04 LTS, Debian 12, or RHEL 9 recommended).
- **Hardware Sizing (Minimum)**:
  - CPU: 2 vCPUs
  - Memory: 4 GB RAM (8 GB recommended for JVM + AI worker concurrency)
  - Disk: 40 GB SSD / NVMe storage
- **Network Requirements**:
  - Inbound ports `80` (HTTP) and `443` (HTTPS) open to the internet.
  - Inbound port `22` (SSH) restricted to administrative IPs.
  - All internal ports (`5432`, `8080`, `8000`, `3000`) blocked from external access.
- **Domain Name**: A fully qualified domain name (FQDN), e.g., `support.example.com`.

---

## B. Server Preparation
1. Update system packages:
   ```bash
   sudo apt-get update && sudo apt-get upgrade -y
   sudo apt-get install -y curl ufw git ca-certificates openssl
   ```
2. Configure basic firewall (UFW):
   ```bash
   sudo ufw default deny incoming
   sudo ufw default allow outgoing
   sudo ufw allow 22/tcp
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw enable
   ```

---

## C. Docker Installation
Install Docker Engine and Docker Compose V2 using official repositories:
```bash
# Add Docker official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable and start service
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

---

## D. Project Installation
Clone the repository to `/opt/smartdesk` (or your preferred production directory):
```bash
sudo mkdir -p /opt/smartdesk
sudo chown -R $USER:$USER /opt/smartdesk
cd /opt/smartdesk
git clone <YOUR_REPOSITORY_URL> .
git checkout <RELEASE_TAG> # e.g., git checkout v1.0.0
```

---

## E. Environment Configuration
Create the private `.env.prod` configuration from the provided safe template:
```bash
cp .env.prod.example .env.prod
chmod 600 .env.prod
```

Configure `.env.prod` with your production parameters:
```ini
DOMAIN_NAME=support.example.com
NEXT_PUBLIC_API_URL=/api/v1
POSTGRES_DB=smartdesk
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<STRONG_GENERATED_DB_PASSWORD>
JWT_SECRET=<STRONG_GENERATED_JWT_SECRET>
RATE_LIMIT_AUTH_PER_MINUTE=30
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
AI_TIMEOUT_MS=3000
BACKUP_RETENTION_DAYS=14
BACKUP_CRON_SCHEDULE=0 2 * * *
BACKUP_ON_STARTUP=false
```

---

## F. Secret Generation
Generate high-entropy cryptographically secure secrets:
```bash
# 1. Generate PostgreSQL Database Password
openssl rand -base64 32

# 2. Generate 256-bit / 384-bit JWT Secret Key
openssl rand -hex 48
```
Paste these values directly into `.env.prod`. **Never commit `.env.prod` to Git.**

---

## G. DNS Configuration
At your DNS registrar or DNS management console, create an Address record:
- **Record Type**: `A` (or `AAAA` for IPv6)
- **Host / Name**: `support` (or `@` for root domain)
- **Target Value**: `<YOUR_PRODUCTION_SERVER_PUBLIC_IP>`
- **TTL**: `300` (5 minutes during initial setup for fast propagation)

Verify DNS resolution before proceeding:
```bash
dig +short support.example.com
```

---

## H. TLS Certificate Configuration

### Production Strategy: Let's Encrypt / ACME
SmartDesk's Nginx configuration includes native support for Let's Encrypt HTTP-01 challenges via `/.well-known/acme-challenge/`:

1. Create required Docker volumes:
   ```bash
   docker volume create smartdesk_prod_certs
   docker volume create smartdesk_prod_acme
   ```
2. Obtain initial certificate using Certbot standalone or webroot:
   ```bash
   docker run --rm \
     -v smartdesk_prod_acme:/var/www/certbot \
     -v smartdesk_prod_certs:/etc/letsencrypt \
     certbot/certbot certonly --webroot \
     --webroot-path=/var/www/certbot \
     -d support.example.com \
     --email admin@example.com --agree-tos --no-eff-email
   ```
3. Link the certificates into the expected Nginx path:
   ```bash
   # Nginx expects fullchain.pem and privkey.pem in /etc/nginx/certs
   # Certbot places them in /etc/letsencrypt/live/support.example.com/
   docker run --rm \
     -v smartdesk_prod_certs:/etc/nginx/certs \
     alpine sh -c "ln -sf /etc/nginx/certs/live/support.example.com/fullchain.pem /etc/nginx/certs/fullchain.pem && ln -sf /etc/nginx/certs/live/support.example.com/privkey.pem /etc/nginx/certs/privkey.pem"
   ```

> [!IMPORTANT]
> **HSTS Preload Warning**: Do NOT enable HSTS preload (`add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"`) immediately. Test HTTPS functionality on your domain for at least 7 days before considering HSTS preloading.

---

## I. Production Image Build
Build production container images using the production Compose specification:
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod build
```
This builds:
- `smartdesk-frontend:prod` with `NEXT_PUBLIC_API_URL=/api/v1`
- `smartdesk-backend:prod` with Java 26 and `application-prod.yml`
- `smartdesk-ai:prod` with Python 3.14 and Scikit-learn pipelines
- `smartdesk-backup:prod` with PostgreSQL 17 client and backup scripts

---

## J. Database Initialization

### Scenario 1: New Production Deployment
1. Start ONLY the database service:
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d postgres
   ```
2. Wait for PostgreSQL to become healthy:
   ```bash
   docker inspect --format="{{.State.Health.Status}}" smartdesk-prod-postgres
   ```
3. Apply database schemas and baseline reference data:
   ```bash
   docker exec -i smartdesk-prod-postgres psql -U postgres -d smartdesk < database/schema.sql
   docker exec -i smartdesk-prod-postgres psql -U postgres -d smartdesk < database/indexes.sql
   docker exec -i smartdesk-prod-postgres psql -U postgres -d smartdesk < database/migrations/002_sentiment_analysis.sql
   docker exec -i smartdesk-prod-postgres psql -U postgres -d smartdesk < database/migrations/003_duplicate_detection.sql
   docker exec -i smartdesk-prod-postgres psql -U postgres -d smartdesk < database/seed.sql
   ```
4. Verify table creation:
   ```bash
   docker exec smartdesk-prod-postgres psql -U postgres -d smartdesk -c "\dt"
   ```

### Scenario 2: Existing Database Migration
- **NEVER** run `schema.sql` or `seed.sql` on an active database containing real customer records.
- **NEVER** run `docker compose down -v` or drop existing volumes.
- Review unapplied migration scripts in `database/migrations/` and apply them incrementally in transactions.

---

## K. Production Startup
Start all platform services:
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

Verify service status:
```bash
docker compose -f docker-compose.prod.yml ps
```
All containers (`nginx`, `frontend`, `backend`, `ai-service`, `postgres`, `backup`) must display `Up (healthy)`.

---

## L. Health Verification
Inspect platform health through the unified Nginx edge:
```bash
# 1. HTTP to HTTPS redirect
curl -I http://support.example.com/
# Expected: HTTP/1.1 301 Moved Permanently, Location: https://support.example.com/

# 2. Sanitized Actuator Health Probe
curl -i https://support.example.com/health
# Expected: HTTP/1.1 200 OK, {"groups":["liveness","readiness"],"status":"UP"}

# 3. Security Headers
curl -I https://support.example.com/
# Expected: X-Content-Type-Options: nosniff, X-Frame-Options: DENY, Referrer-Policy: strict-origin-when-cross-origin
```

---

## M. Login Verification
Verify authentication through Nginx:
```bash
curl -X POST https://support.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@smartdesk.local","password":"Password123!"}'
# Expected: HTTP 200 with JWT Bearer token
```

---

## N. Ticket Workflow Verification
1. Access `https://support.example.com/login` in a web browser.
2. Sign in as Customer (`alex@acmecorp.local` / `Password123!`).
3. Create a test ticket (`subject: Production Billing Inquiry`).
4. Confirm ticket is created and automatically routed to Billing team.
5. Sign in as Agent (`agent.billing@smartdesk.local` / `Password123!`).
6. Open ticket, post reply, and resolve ticket.

---

## O. Backup Verification
Execute a manual backup test:
```bash
# Trigger immediate backup run
docker exec smartdesk-prod-backup /usr/local/bin/backup.sh

# Verify non-empty .sql.gz created in backup volume
docker exec smartdesk-prod-backup ls -lh /backups/

# Verify backup integrity
docker exec smartdesk-prod-backup sh -c "gzip -t /backups/*.sql.gz && echo 'BACKUP_VALID'"
```

---

## P. Monitoring
- **Container Health**: `docker ps --format "table {{.Names}}\t{{.Status}}"`
- **Container Resource Utilization**: `docker stats --no-stream`
- **Application Logs**:
  ```bash
  docker compose -f docker-compose.prod.yml logs --tail=100 -f backend
  docker compose -f docker-compose.prod.yml logs --tail=100 -f ai-service
  docker compose -f docker-compose.prod.yml logs --tail=100 -f nginx
  ```
- **Recommended Future Observability**: Export Prometheus metrics from `/actuator/prometheus` to a centralized Grafana instance.

---

## Q. Shutdown Procedure
To safely stop the production environment without data loss:
```bash
# Graceful stop
docker compose -f docker-compose.prod.yml stop
```
> [!CAUTION]
> **NEVER RUN `docker compose down -v`**. The `-v` flag deletes named volumes, destroying PostgreSQL database files and historical backups.

---

## R. Restart Procedure
To restart individual services or the entire stack:
```bash
# Restart entire stack
docker compose -f docker-compose.prod.yml restart

# Restart specific service (e.g., after certificate renewal)
docker compose -f docker-compose.prod.yml restart nginx
```

---

## S. Rollback Procedure
If a newly deployed image introduces a defect:
1. Re-tag or revert image tag in `docker-compose.prod.yml`:
   ```bash
   # Example: revert to previous image release
   docker tag smartdesk-backend:v1.0.0 smartdesk-backend:prod
   docker compose -f docker-compose.prod.yml up -d backend
   ```
2. Keep the database intact.
3. Validate `/health` and login functionality.

---

## T. Disaster Recovery
If the server suffers a catastrophic failure:
1. Provision a replacement Linux server following sections A through D.
2. Transfer the latest `.sql.gz` backup file from off-host storage into `smartdesk_prod_backups` volume.
3. Follow the restore runbook in `docs/database-backup-restore.md`.
4. Update DNS to point to the new server IP.
5. Re-issue TLS certificates.
