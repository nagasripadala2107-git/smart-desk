#!/usr/bin/env bash
# ==============================================================================
# SmartDesk Backup Container Entrypoint
# ==============================================================================
# Executes scheduled backups using crond or sleep interval.
# Default schedule: Daily at 02:00 AM UTC.
# ==============================================================================

set -euo pipefail

BACKUP_CRON_SCHEDULE="${BACKUP_CRON_SCHEDULE:-0 2 * * *}"

echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] Initializing SmartDesk Backup Scheduler..."
echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] Backup schedule: ${BACKUP_CRON_SCHEDULE}"

# Ensure backup script is executable
chmod +x /usr/local/bin/backup.sh

# Perform an initial backup if requested via BACKUP_ON_STARTUP=true
if [[ "${BACKUP_ON_STARTUP:-false}" == "true" ]]; then
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] Running initial startup backup..."
    /usr/local/bin/backup.sh || echo "[$(date +'%Y-%m-%d %H:%M:%S')] [WARN] Initial startup backup failed."
fi

# Configure crontab
echo "${BACKUP_CRON_SCHEDULE} /usr/local/bin/backup.sh >> /var/log/cron.log 2>&1" > /etc/crontabs/root

touch /var/log/cron.log

echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] Starting crond in foreground..."
exec crond -f -d 8
