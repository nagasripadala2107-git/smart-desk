# SmartDesk Database Backup & Restore Runbook

This document defines the automated backup routine, archive verification checks, disaster recovery procedures, and controlled database restoration workflow for **SmartDesk**.

---

## 1. Automated Backup Architecture

SmartDesk includes a dedicated, containerized backup microservice (`smartdesk-prod-backup`) running alongside PostgreSQL:

- **Engine**: PostgreSQL 17 Alpine Client
- **Backup Mechanism**: `pg_dump` executed via `/usr/local/bin/backup.sh`
- **Output Format**: Gzip-compressed SQL dump (`smartdesk_backup_%Y%m%d_%H%M%S.sql.gz`)
- **Storage Volume**: Named Docker volume `smartdesk_prod_backups` mounted to `/backups` inside the container.
- **Schedule**: Cron daemon executing nightly at 02:00 UTC (`0 2 * * *`).
- **Retention Policy**: Automated pruning removes backup archives older than 14 days (`BACKUP_RETENTION_DAYS=14`).
- **Security**: Database credentials supplied via environment variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`); no plaintext passwords in logs or script parameters.

---

## 2. On-Demand / Manual Backup Procedure

To trigger an immediate backup before upgrades, schema changes, or maintenance:

```bash
# 1. Execute the backup script inside the backup container
docker exec smartdesk-prod-backup /usr/local/bin/backup.sh

# 2. Verify the newly created archive file
docker exec smartdesk-prod-backup ls -lh /backups/

# 3. Test archive integrity
docker exec smartdesk-prod-backup sh -c "gzip -t /backups/*.sql.gz && echo 'GZIP_INTEGRITY_PASSED'"
```

---

## 3. Backup Inspection & Content Verification

To inspect the contents of a backup archive without restoring it:

```bash
# View archive header information and SQL metadata
docker exec smartdesk-prod-backup sh -c "zcat /backups/smartdesk_backup_<TIMESTAMP>.sql.gz | head -n 30"

# Verify table definitions are included
docker exec smartdesk-prod-backup sh -c "zcat /backups/smartdesk_backup_<TIMESTAMP>.sql.gz | grep 'CREATE TABLE'"
```

---

## 4. Controlled Restoration Procedure (Isolated Sandbox First)

> [!CAUTION]
> **CRITICAL RULE**: NEVER restore a backup directly over the live production database as the primary verification test. A corrupted or incomplete dump could overwrite operational customer data.

Always follow this **isolated sandbox restoration flow**:

```text
[ Backup Archive (.sql.gz) ]
             │
             ▼
[ Temporary Isolated PostgreSQL Container ]
             │
             ├── 1. Schema integrity validation
             ├── 2. Row count reconciliation
             ├── 3. Critical table checks (users, tickets, audit_logs)
             └── 4. Application connection compatibility test
             │
      (Validation Successful)
             │
             ▼
[ Planned Controlled Production Recovery ]
```

### Step 1: Spin Up a Temporary Sandbox PostgreSQL Container
Create an isolated PostgreSQL container attached to an ephemeral volume:
```bash
docker run -d \
  --name smartdesk-restore-test \
  -e POSTGRES_DB=smartdesk \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=restore_test_password \
  postgres:17
```

Wait until the test container is healthy:
```bash
docker exec smartdesk-restore-test pg_isready -U postgres -d smartdesk
```

### Step 2: Stream and Decompress Backup into Sandbox Container
Extract the backup archive from the backup volume and pipe it directly into the sandbox database:
```bash
docker exec smartdesk-prod-backup zcat /backups/smartdesk_backup_<TIMESTAMP>.sql.gz | \
  docker exec -i smartdesk-restore-test psql -U postgres -d smartdesk
```

### Step 3: Verify Schema & Table Presence in Sandbox
Confirm all 19 core tables exist:
```bash
docker exec smartdesk-restore-test psql -U postgres -d smartdesk -c "
SELECT count(*) FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
"
# Expected count: 19
```

### Step 4: Verify Row Counts & Entity Integrity
Validate counts against known operational baselines:
```bash
docker exec smartdesk-restore-test psql -U postgres -d smartdesk -c "
SELECT 'users' as tbl, count(*) FROM users
UNION ALL SELECT 'customers', count(*) FROM customers
UNION ALL SELECT 'agents', count(*) FROM agents
UNION ALL SELECT 'teams', count(*) FROM teams
UNION ALL SELECT 'categories', count(*) FROM categories
UNION ALL SELECT 'tickets', count(*) FROM tickets
UNION ALL SELECT 'ticket_messages', count(*) FROM ticket_messages
UNION ALL SELECT 'ticket_events', count(*) FROM ticket_events
UNION ALL SELECT 'ticket_sentiment_analysis', count(*) FROM ticket_sentiment_analysis
UNION ALL SELECT 'ticket_duplicate_matches', count(*) FROM ticket_duplicate_matches
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs;
"
```

### Step 5: Clean Up Sandbox Container
Once verification is complete:
```bash
docker stop smartdesk-restore-test
docker rm smartdesk-restore-test
```

---

## 5. Live Production Disaster Recovery (Planned Overwrite)

If the production database has experienced catastrophic data corruption or disk loss and has already been verified via Sandbox:

1. **Stop Application Traffic**:
   ```bash
   docker compose -f docker-compose.prod.yml stop backend frontend
   ```
2. **Take a Pre-Restoration Snapshot** of current state (if disk allows):
   ```bash
   docker exec smartdesk-prod-backup /usr/local/bin/backup.sh
   ```
3. **Drop and Recreate Clean Database**:
   ```bash
   docker exec -i smartdesk-prod-postgres psql -U postgres -c "DROP DATABASE IF EXISTS smartdesk;"
   docker exec -i smartdesk-prod-postgres psql -U postgres -c "CREATE DATABASE smartdesk WITH OWNER postgres;"
   ```
4. **Restore Verified Backup Archive**:
   ```bash
   docker exec smartdesk-prod-backup zcat /backups/smartdesk_backup_<VERIFIED_TIMESTAMP>.sql.gz | \
     docker exec -i smartdesk-prod-postgres psql -U postgres -d smartdesk
   ```
5. **Restart Platform Services**:
   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```
6. **Verify Health Endpoint**:
   ```bash
   curl -i https://support.example.com/health
   ```

---

## 6. Future Disaster Recovery Recommendations

* **Current Implementation**: Backups are stored locally within the `smartdesk_prod_backups` Docker volume on the host server.
* **Production Recommendation**: Real-world disaster recovery requires **off-host replication**. A scheduled cron job or cloud storage sync tool (e.g., AWS S3 CLI, Google Cloud Storage `gsutil`, or Azure Blob CLI) should replicate `.sql.gz` files from `/backups` to an encrypted, geographically distinct cloud bucket nightly.
