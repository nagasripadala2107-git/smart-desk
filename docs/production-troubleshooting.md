# SmartDesk Production Incident Troubleshooting Guide

This guide provides operational runbooks for resolving production incidents within the SmartDesk system.
All recovery procedures strictly adhere to zero-data-loss standards. **Never execute destructive database commands (e.g., `DROP TABLE`, `TRUNCATE`, `docker compose down -v`) during incident triage.**

---

## Triage Workflow Matrix

| Incident Category | Severity | Primary Health Endpoint / Indicator |
| :--- | :--- | :--- |
| 1. Nginx Unavailable | Critical | `curl -k https://<domain>/health` connection refused / 502 Bad Gateway |
| 2. Frontend Unavailable | High | `https://<domain>/` returns 502 / blank page |
| 3. Backend Unavailable | Critical | `https://<domain>/health` fails or `/api/v1/*` returns 502 / 503 |
| 4. PostgreSQL Unavailable | Critical | Backend crash loop, `PSQLException`, Hikari connection timeout |
| 5. AI Service Unavailable | Moderate | Logs show `AI service communication error`, tickets route to fallback queue |
| 6. Login Failures | High | HTTP 401 Unauthorized, HTTP 429 Too Many Requests |
| 7. Ticket Creation Failures | High | HTTP 400 Bad Request, HTTP 500 Internal Server Error on POST `/api/v1/tickets` |
| 8. AI Classification Unavailable | Moderate | Tickets created with `category: null` or fallback queue |
| 9. Sentiment Unavailable | Moderate | `ticket_sentiment_analysis` table lacks entries for new messages |
| 10. Duplicate Detection Unavailable | Moderate | Duplicate candidate lists empty on agent dashboard |
| 11. Backup Failure | High | Backup container exit code != 0, missing `.dump` in `/backups` |
| 12. Disk Full | Critical | Container crash, PostgreSQL read-only / write failure, disk I/O errors |
| 13. TLS Certificate Problems | High | Browser `ERR_CERT_DATE_INVALID` or SSL handshake termination |
| 14. Connection Pool Exhaustion | High | HikariCP `ConnectionTimeoutException: Connection is not available` |

---

## 1. Nginx Unavailable

### Symptoms
- Browsers receive `ERR_CONNECTION_REFUSED` or `502 Bad Gateway` on all endpoints.
- External monitoring reports HTTP/HTTPS port 80/443 unreachable.

### Checks
1. Check container status:
   ```bash
   docker compose -f docker-compose.prod.yml ps smartdesk-nginx-prod
   ```
2. Inspect Nginx log output:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=100 smartdesk-nginx-prod
   ```
3. Test Nginx configuration syntax inside container:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-nginx-prod nginx -t
   ```
4. Verify host port bindings:
   ```bash
   netstat -tlpn | grep -E ':(80|443)'
   ```

### Likely Causes
- Host port 80 or 443 conflict with another process (Apache, host Nginx, IIS).
- Configuration syntax error in `docker/nginx/nginx.conf`.
- Missing TLS certificates at `/etc/nginx/certs/fullchain.pem` or `/etc/nginx/certs/privkey.pem`.

### Safe Recovery Action
- Correct syntax errors in `docker/nginx/nginx.conf`.
- Verify certificate paths mounted from `./certs`.
- Restart Nginx container safely:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-nginx-prod
  ```

---

## 2. Frontend Unavailable

### Symptoms
- Requests to `https://<domain>/` return HTTP 502 Bad Gateway or 504 Gateway Timeout.
- Direct API calls (`/api/v1/*`) succeed, but web UI does not render.

### Checks
1. Check Next.js container status:
   ```bash
   docker compose -f docker-compose.prod.yml ps smartdesk-frontend-prod
   ```
2. Inspect Next.js runtime logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=100 smartdesk-frontend-prod
   ```
3. Test internal frontend accessibility from Nginx container:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-nginx-prod curl -I http://frontend:3000/
   ```

### Likely Causes
- Next.js Node process crashed due to Out-Of-Memory (OOM) or unhandled runtime exception.
- Standalone build artifacts missing or corrupted during image generation.
- Internal network DNS resolution issue between Nginx and frontend container.

### Safe Recovery Action
- If container exited or OOM killed, restart the service:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-frontend-prod
  ```
- If image was misbuilt, rebuild and restart frontend service without touching database:
  ```bash
  docker compose -f docker-compose.prod.yml build frontend
  docker compose -f docker-compose.prod.yml up -d --no-deps frontend
  ```

---

## 3. Backend Unavailable

### Symptoms
- `curl https://<domain>/health` fails or returns 502/503.
- Frontend displays "Network Error" or "Unable to reach server" notifications.
- All `/api/v1/*` endpoints fail.

### Checks
1. Check Spring Boot container status:
   ```bash
   docker compose -f docker-compose.prod.yml ps smartdesk-backend-prod
   ```
2. Inspect Spring Boot logs for startup failures or JVM crashes:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=150 smartdesk-backend-prod
   ```
3. Test Actuator health directly from within internal network:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-nginx-prod curl -s http://backend:8080/actuator/health
   ```

### Likely Causes
- PostgreSQL container down or unreachable during startup (DataSource initialization failed).
- JVM OOM (check Docker memory limits or JVM heap flags).
- Invalid environment variables (e.g. invalid `SPRING_PROFILES_ACTIVE`, database credentials, or secret format).

### Safe Recovery Action
- Verify PostgreSQL is healthy first: `docker compose -f docker-compose.prod.yml ps postgres`
- Verify `.env.prod` credentials match PostgreSQL configuration.
- Restart backend service:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-backend-prod
  ```

---

## 4. PostgreSQL Unavailable

### Symptoms
- Backend fails health checks and logs: `org.postgresql.util.PSQLException: Connection refused`.
- Backend enters crash loop (`Restarting`).

### Checks
1. Check PostgreSQL container status:
   ```bash
   docker compose -f docker-compose.prod.yml ps smartdesk-postgres-prod
   ```
2. Inspect PostgreSQL engine logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=100 smartdesk-postgres-prod
   ```
3. Test direct database connection via `pg_isready`:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-postgres-prod pg_isready -U postgres
   ```
4. Verify host disk space:
   ```bash
   df -h
   ```

### Likely Causes
- Host disk full preventing PostgreSQL write-ahead log (WAL) allocation.
- Incompatible PostgreSQL volume permissions or corrupted lock file (`postmaster.pid`).
- Container stopped due to out-of-memory killer.

### Safe Recovery Action
- **NEVER** run `docker compose down -v` or delete `smartdesk_postgres_data`.
- If disk is full, clean temporary Docker build cache or logs (see Section 12).
- Restart PostgreSQL service safely:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-postgres-prod
  ```
- Wait 15 seconds for recovery, then restart backend service if needed.

---

## 5. AI Service Unavailable

### Symptoms
- Tickets are created successfully, but category suggestions, sentiment scores, and duplicate flags are missing (`null`).
- Backend logs report: `AI service unavailable or returned error. Fallback activated.`

### Checks
1. Check AI service container status:
   ```bash
   docker compose -f docker-compose.prod.yml ps smartdesk-ai-prod
   ```
2. Inspect Python FastAPI logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=100 smartdesk-ai-prod
   ```
3. Test AI internal health check endpoint:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-backend-prod curl -s http://ai-service:8000/health
   ```

### Likely Causes
- ML model serialization artifacts missing or corrupted at `/app/models/*.joblib`.
- AI container memory limit exceeded during scikit-learn batch prediction.
- Uvicorn worker crashed.

### Safe Recovery Action
- SmartDesk core ticket ingestion is designed with graceful degradation: ticket workflows continue even if AI is offline.
- Restart AI container:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-ai-prod
  ```
- Verify model readiness via `curl http://ai-service:8000/health`.

---

## 6. Login Failures

### Symptoms
- Users receive `401 Unauthorized` or `429 Too Many Requests` when submitting credentials.
- Users report valid credentials no longer work.

### Checks
1. Inspect authentication logs in backend:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=100 smartdesk-backend-prod | grep -E '(auth|login|JWT)'
   ```
2. Check for rate-limiting triggers (HTTP 429):
   - Rate limit enforces 30 requests/minute per client IP.
3. Check user record status in PostgreSQL:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-postgres-prod psql -U postgres -d smartdesk -c "SELECT email, role, is_active FROM users WHERE email='user@example.com';"
   ```

### Likely Causes
- Client IP exceeded rate limit (30 requests/min).
- Account deactivated (`is_active = FALSE`).
- Clock skew between host and client causing premature JWT expiration.
- Mismatched `JWT_SECRET` following an uncoordinated container restart.

### Safe Recovery Action
- If rate-limited, wait 60 seconds for bucket replenishment.
- If user account was deactivated, reactivate via Admin console or authorized database query:
  ```sql
  UPDATE users SET is_active = TRUE WHERE email = 'user@example.com';
  ```
- Ensure system time is synchronized via NTP.

---

## 7. Ticket Creation Failures

### Symptoms
- POST to `/api/v1/tickets` returns HTTP 400 Bad Request or HTTP 500.
- Customers receive "Ticket submission failed" error in web portal.

### Checks
1. Inspect backend error logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs --tail=150 smartdesk-backend-prod | grep -i "ticket"
   ```
2. Verify customer ID exists and belongs to authenticated user:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-postgres-prod psql -U postgres -d smartdesk -c "SELECT u.id, u.email, c.id AS customer_id FROM users u LEFT JOIN customers c ON c.user_id = u.id WHERE u.email = 'customer@example.com';"
   ```
3. Inspect database foreign key integrity and constraints.

### Likely Causes
- Authenticated user has role `CUSTOMER` but lacks a linked `customers` profile table row.
- Input validation failure (title > 255 chars, description empty, invalid priority).
- Database constraint violation or table lock.

### Safe Recovery Action
- Verify input conforms to DTO constraints.
- If customer profile row is missing, insert corresponding profile row associated with `user_id`.
- Review backend stack trace in logs (internal details are suppressed from client responses).

---

## 8. AI Classification Unavailable

### Symptoms
- Newly submitted tickets default to `GENERAL` category or unassigned routing.
- Backend logs show warning: `Category prediction failed: using default routing`.

### Checks
1. Check classification endpoint response directly:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-backend-prod curl -s -X POST http://ai-service:8000/predict/category -H "Content-Type: application/json" -d '{"text":"VPN connection dropped repeatedly"}'
   ```
2. Verify model file exists inside AI container:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-ai-prod ls -la /app/models/
   ```

### Likely Causes
- Model artifact `category_model.joblib` or `vectorizer.joblib` corrupted or unreadable.
- Input text empty or containing only stop words / whitespace.

### Safe Recovery Action
- Restart AI microservice:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-ai-prod
  ```
- Fallback routing logic safely assigns tickets to general support queue; no tickets are dropped.

---

## 9. Sentiment Unavailable

### Symptoms
- `ticket_sentiment_analysis` rows are not generated.
- Agent dashboard sentiment badge displays "UNASSESSED" or empty.

### Checks
1. Test sentiment endpoint directly:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-backend-prod curl -s -X POST http://ai-service:8000/predict/sentiment -H "Content-Type: application/json" -d '{"text":"System is broken and I am very unhappy"}'
   ```
2. Check backend asynchronous sentiment listener logs.

### Likely Causes
- Sentiment analysis runs asynchronously or via scheduled hook; thread pool exhaustion may delay execution.
- AI service timeout exceeded (default 5000ms).

### Safe Recovery Action
- Sentiment analysis is purely advisory and non-blocking.
- Verify AI service responsiveness. If latency is high, restart AI container.

---

## 10. Duplicate Detection Unavailable

### Symptoms
- Agent ticket details panel does not show "Potential Duplicate Tickets".
- Backend logs: `Duplicate detection search completed with 0 matches` or timeout.

### Checks
1. Test duplicate endpoint:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-backend-prod curl -s -X POST http://ai-service:8000/predict/duplicates -H "Content-Type: application/json" -d '{"ticket_id":"<UUID>","text":"Sample problem text"}'
   ```
2. Verify candidate ticket corpus size in database.

### Likely Causes
- Cosine similarity threshold (0.75) not met by existing open tickets (normal behavior).
- AI vectorizer model failed to vectorize candidate text.

### Safe Recovery Action
- Confirm whether candidate tickets exist in identical category.
- Restart AI container if model vectorizer process is unresponsive.

---

## 11. Backup Failure

### Symptoms
- Daily backup file not generated in `/backups` directory.
- `docker compose -f docker-compose.prod.yml ps smartdesk-backup-prod` shows exit status != 0.

### Checks
1. Inspect backup container logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs smartdesk-backup-prod
   ```
2. Check backup storage permissions and remaining disk capacity:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-backup-prod ls -la /backups
   ```
3. Test manual on-demand backup execution:
   ```bash
   docker compose -f docker-compose.prod.yml run --rm backup /scripts/backup.sh
   ```

### Likely Causes
- Backup destination directory permissions prevent writing.
- Insufficient disk space on backup mount volume.
- PostgreSQL authentication failure for backup user.

### Safe Recovery Action
- Clear expired backups or expand host disk volume.
- Correct directory permissions (`chmod 700 /backups`).
- Trigger manual verification backup using the procedure in `docs/database-backup-restore.md`.

---

## 12. Disk Full

### Symptoms
- PostgreSQL fails write transactions: `PANIC: could not write to log file: No space left on device`.
- Docker daemon cannot allocate container layers or write container logs.

### Checks
1. Inspect host filesystem utilization:
   ```bash
   df -h
   ```
2. Inspect Docker disk utilization:
   ```bash
   docker system df
   ```
3. Find large log files:
   ```bash
   du -sh /var/lib/docker/containers/*/*-json.log 2>/dev/null | sort -rh | head -n 10
   ```

### Likely Causes
- Unbounded Docker container logs consuming root partition.
- Accumulated orphan Docker images and build caches.
- High volume of uncompressed database backups.

### Safe Recovery Action
- **DO NOT** delete `/var/lib/docker/volumes/smartdesk_postgres_data`.
- Clean unused Docker build cache and stopped temporary containers:
  ```bash
  docker builder prune -f
  docker image prune -f
  ```
- Truncate excessively large container logs safely:
  ```bash
  truncate -s 0 /var/lib/docker/containers/*/*-json.log
  ```
- Ensure log rotation is configured in `docker-compose.prod.yml` (`max-size: "20m"`, `max-file: "5"`).

---

## 13. TLS Certificate Problems

### Symptoms
- Browsers flag connection with `NET::ERR_CERT_COMMON_NAME_INVALID` or `ERR_CERT_DATE_INVALID`.
- HTTPS requests fail; HTTP port 80 redirects to expired HTTPS certificate.

### Checks
1. Inspect certificate expiration date via OpenSSL:
   ```bash
   openssl x509 -in ./certs/fullchain.pem -noout -dates -subject -issuer
   ```
2. Verify certificate domain matches incoming hostname:
   ```bash
   openssl x509 -in ./certs/fullchain.pem -noout -ext subjectAltName
   ```
3. Inspect Nginx SSL error logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs smartdesk-nginx-prod | grep -i ssl
   ```

### Likely Causes
- Let's Encrypt 90-day certificate expired without automated renewal.
- Certificate renewed on host filesystem but Nginx was not reloaded to load new files.
- Staging self-signed certificate deployed in production environment by mistake.

### Safe Recovery Action
- Renew certificate using Certbot or ACME client:
  ```bash
  certbot certonly --webroot -w ./certbot -d <your-domain>
  ```
- Reload Nginx without downtime:
  ```bash
  docker compose -f docker-compose.prod.yml exec smartdesk-nginx-prod nginx -s reload
  ```

---

## 14. Database Connection Pool Exhaustion

### Symptoms
- Backend logs: `org.springframework.dao.DataAccessResourceFailureException: Unable to acquire JDBC Connection`.
- `HikariPool-1 - Connection is not available, request timed out after 30000ms`.
- High HTTP request latency (> 30s) followed by 500 errors.

### Checks
1. Check active database connections in PostgreSQL:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-postgres-prod psql -U postgres -d smartdesk -c "SELECT count(*), state FROM pg_stat_activity GROUP BY state;"
   ```
2. Check for long-running blocking transactions:
   ```bash
   docker compose -f docker-compose.prod.yml exec smartdesk-postgres-prod psql -U postgres -d smartdesk -c "SELECT pid, now() - query_start AS duration, query, state FROM pg_stat_activity WHERE state != 'idle' ORDER BY duration DESC LIMIT 5;"
   ```

### Likely Causes
- Traffic spike exceeding configured Hikari maximum pool size (default: 20).
- Unindexed query or slow lock holding connection open.
- Connection leak due to unclosed transaction or external API call inside `@Transactional` block.

### Safe Recovery Action
- Terminate hanging non-idle queries if blocking production:
  ```sql
  SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid = <PID>;
  ```
- Adjust Hikari pool settings in `.env.prod` / `application-prod.yml` if legitimate concurrency requires higher pool capacity:
  ```env
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30
  ```
- Restart backend to re-establish clean connection pool:
  ```bash
  docker compose -f docker-compose.prod.yml restart smartdesk-backend-prod
  ```
