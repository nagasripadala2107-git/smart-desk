#!/usr/bin/env bash
# ==============================================================================
# SmartDesk Production PostgreSQL Automated Backup Script
# ==============================================================================
# Generates timestamped, compressed database dumps using pg_dump.
# Enforces secret safety: credentials passed via libpq environment variables.
# Manages rolling retention, automatically removing backups older than 14 days.
# ==============================================================================

set -euo pipefail

# ------------------------------------------------------------------------------
# Configuration & Defaults
# ------------------------------------------------------------------------------
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"
BACKUP_FILE="${BACKUP_DIR}/smartdesk_backup_${TIMESTAMP}.sql.gz"
BACKUP_TMP="${BACKUP_FILE}.tmp"

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] $*"
}

log_error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [ERROR] $*" >&2
}

# ------------------------------------------------------------------------------
# Validation
# ------------------------------------------------------------------------------
if [[ -z "${PGHOST:-}" || -z "${PGUSER:-}" || -z "${PGDATABASE:-}" ]]; then
    log_error "Missing required PostgreSQL environment variables (PGHOST, PGUSER, PGDATABASE)."
    exit 1
fi

mkdir -p "${BACKUP_DIR}"

log "Starting database backup for database '${PGDATABASE}' on host '${PGHOST}:${PGPORT:-5432}'..."

# ------------------------------------------------------------------------------
# Backup Execution
# ------------------------------------------------------------------------------
# Credentials read strictly from PGPASSWORD environment variable (never CLI args)
if pg_dump \
    -h "${PGHOST}" \
    -p "${PGPORT:-5432}" \
    -U "${PGUSER}" \
    -d "${PGDATABASE}" \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges | gzip -9 > "${BACKUP_TMP}"; then

    mv "${BACKUP_TMP}" "${BACKUP_FILE}"
    BACKUP_SIZE="$(du -h "${BACKUP_FILE}" | cut -f1)"
    log "Backup completed successfully: ${BACKUP_FILE} (${BACKUP_SIZE})"
else
    log_error "pg_dump failed during backup generation."
    rm -f "${BACKUP_TMP}"
    exit 1
fi

# ------------------------------------------------------------------------------
# Retention Pruning (Remove backups older than 14 days)
# ------------------------------------------------------------------------------
log "Checking for backups older than ${RETENTION_DAYS} days in ${BACKUP_DIR}..."
PRUNED_COUNT=$(find "${BACKUP_DIR}" -type f -name "smartdesk_backup_*.sql.gz" -mtime +"${RETENTION_DAYS}" | wc -l)

if [[ "${PRUNED_COUNT}" -gt 0 ]]; then
    find "${BACKUP_DIR}" -type f -name "smartdesk_backup_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete
    log "Pruned ${PRUNED_COUNT} expired backup file(s)."
else
    log "No expired backups found to prune."
fi

# ------------------------------------------------------------------------------
# Summary
# ------------------------------------------------------------------------------
TOTAL_BACKUPS=$(find "${BACKUP_DIR}" -type f -name "smartdesk_backup_*.sql.gz" | wc -l)
log "Backup cycle finished. Current active backups stored: ${TOTAL_BACKUPS}."
exit 0
