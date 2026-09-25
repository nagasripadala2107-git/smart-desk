# ==========================================
# Next.js Production Multi-Stage Dockerfile
# Base: Node 26 Alpine
# ==========================================

# ------------------------------------------
# Stage 1: Dependencies Cache
# ------------------------------------------
FROM node:26-alpine AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

# Build-time proxy configuration for Docker Desktop environments
ARG HTTP_PROXY="http://http.docker.internal:3128"
ARG HTTPS_PROXY="http://http.docker.internal:3128"
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY}

# Install dependencies using package-lock.json with socket concurrency limit to prevent proxy connection exhaustion
COPY package.json package-lock.json ./
RUN npm config set maxsockets 3 && \
    npm config set fetch-retries 5 && \
    npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000 && \
    npm ci --prefer-offline --no-audit

# ------------------------------------------
# Stage 2: Production Build
# ------------------------------------------
FROM node:26-alpine AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Browser API URL for client-side bundle generation
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL} \
    NEXT_TELEMETRY_DISABLED=1

# Build Next.js standalone application
RUN npm run build

# ------------------------------------------
# Stage 3: Minimal Production Runtime
# ------------------------------------------
FROM node:26-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME="0.0.0.0"

# Dedicated non-root user and group
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy static assets and standalone build output
COPY --from=builder --chown=nextjs:nodejs /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Switch to non-root execution
USER nextjs

EXPOSE 3000

# Docker Healthcheck targeting Next.js HTTP server via Node native HTTP client
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3000/', (res) => process.exit(res.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1));"

# Launch Next.js standalone production server
CMD ["node", "server.js"]
