package com.smartdesk.security;

import com.smartdesk.dto.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Deque<Long>> requestCounts = new ConcurrentHashMap<>();

    // Default rate limit: 30 requests per minute per IP on sensitive auth endpoints
    private final int maxRequestsPerMinute;
    private final long windowMillis = 60_000L;

    public RateLimitingFilter(@org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.auth:30}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public RateLimitingFilter() {
        this(30);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Rate limit applies only to sensitive authentication endpoints
        if (isRateLimitedPath(path)) {
            String clientIp = getClientIp(request);
            long now = Instant.now().toEpochMilli();

            if (!isAllowed(clientIp, now)) {
                sendRateLimitResponse(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return path != null && (
                path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/register")
        );
    }

    private synchronized boolean isAllowed(String clientIp, long now) {
        Deque<Long> timestamps = requestCounts.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        // Evict expired timestamps outside sliding window
        long cutoff = now - windowMillis;
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequestsPerMinute) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    private void sendRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String json = "{\"timestamp\":\"" + java.time.OffsetDateTime.now() + "\","
                + "\"status\":429,"
                + "\"error\":\"TOO_MANY_REQUESTS\","
                + "\"message\":\"Rate limit exceeded. Please wait a moment before trying again.\","
                + "\"path\":\"" + request.getRequestURI() + "\","
                + "\"validationErrors\":null}";

        response.getWriter().write(json);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    public void reset() {
        requestCounts.clear();
    }
}
