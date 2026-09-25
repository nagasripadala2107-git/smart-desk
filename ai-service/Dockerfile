# ==========================================
# SmartDesk AI Microservice Dockerfile
# Python 3.14-slim Base Image
# ==========================================
FROM python:3.14-slim

# Build-time proxy support for Docker Desktop / internal network environments
ARG HTTP_PROXY=""
ARG HTTPS_PROXY=""
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Install project dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Reset proxy environment variables for runtime
ENV HTTP_PROXY="" \
    HTTPS_PROXY=""

# Create a dedicated non-root application user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1001 -d /app appuser

# Copy application runtime source and trained classifier model
COPY app/ ./app/
COPY models/ ./models/

# Secure directory ownership for non-root execution
RUN chown -R appuser:appgroup /app

# Switch to non-root execution
USER appuser

# Expose microservice HTTP port
EXPOSE 8000

# Native Python healthcheck inspecting /health endpoint
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; res = urllib.request.urlopen('http://localhost:' + str(__import__('os').environ.get('PORT', 8000)) + '/health'); exit(0 if res.getcode() == 200 else 1)"

# Start FastAPI application with Uvicorn (supporting dynamic PORT for Render)
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
