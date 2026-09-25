# ==============================================================================
# SmartDesk Backup Service Dockerfile
# ==============================================================================
FROM postgres:17-alpine

# Install bash, gzip, and findutils for retention scripts
RUN apk add --no-cache bash gzip findutils tzdata

WORKDIR /backups

COPY docker/backup/backup.sh /usr/local/bin/backup.sh
COPY docker/backup/entrypoint.sh /usr/local/bin/entrypoint.sh

RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
