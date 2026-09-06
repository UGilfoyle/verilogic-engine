package com.verilogic.ui.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise DDoS &amp; Volumetric Flooding Defense Filter.
 * Implements a high-performance, lock-free Sliding-Window Token Bucket algorithm.
 * Time Complexity: O(1), Space Complexity: O(1) per client IP.
 * Emits standard rate limit headers (X-RateLimit-*) visible in network inspectors.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DdosRateLimiterFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DdosRateLimiterFilter.class);

    public static final int DEFAULT_MAX_REQUESTS_PER_SECOND = 100;
    public static final int BURST_CAPACITY = 150;

    private final Map<String, TokenBucket> bucketRegistry = new ConcurrentHashMap<>();
    private final ClientIpResolver clientIpResolver;

    public DdosRateLimiterFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    private static class TokenBucket {
        private final AtomicLong lastRefillTimestampNanos = new AtomicLong(System.nanoTime());
        private final AtomicInteger availableTokens = new AtomicInteger(BURST_CAPACITY);

        public boolean tryConsume() {
            refill();
            int current;
            do {
                current = availableTokens.get();
                if (current <= 0) {
                    return false;
                }
            } while (!availableTokens.compareAndSet(current, current - 1));
            return true;
        }

        public int getAvailableTokens() {
            refill();
            return Math.max(0, availableTokens.get());
        }

        private void refill() {
            long now = System.nanoTime();
            long last = lastRefillTimestampNanos.get();
            long elapsedNanos = now - last;

            // Refill tokens based on elapsed time (1 token per 10ms = 100 tokens/sec)
            long newTokens = elapsedNanos / 10_000_000L;
            if (newTokens > 0 && lastRefillTimestampNanos.compareAndSet(last, now)) {
                availableTokens.updateAndGet(tokens -> (int) Math.min(BURST_CAPACITY, tokens + newTokens));
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String path = httpRequest.getRequestURI();

            // Bypass rate limiting for internal Vaadin frontend assets and web sockets
            if (path.startsWith("/VAADIN/") || path.startsWith("/frontend/") || path.endsWith(".ico") || path.endsWith(".js")) {
                chain.doFilter(request, response);
                return;
            }

            String clientIp = clientIpResolver.resolve(httpRequest);
            TokenBucket bucket = bucketRegistry.computeIfAbsent(clientIp, k -> new TokenBucket());

            int remainingTokens = bucket.getAvailableTokens();
            httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_MAX_REQUESTS_PER_SECOND));
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(remainingTokens));
            httpResponse.setHeader("X-RateLimit-Reset-Seconds", "1");

            if (!bucket.tryConsume()) {
                log.warn("[SECURITY WARN] [429 TOO_MANY_REQUESTS] DDoS Rate Limit Exceeded for IP [{}] on URI [{}]. Available tokens: 0", 
                        clientIp, path);

                httpResponse.setStatus(429); // HTTP 429 Too Many Requests
                httpResponse.setHeader("Retry-After", "1");
                httpResponse.setContentType("application/json");

                String jsonError = String.format(
                        "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"code\":\"DDOS_RATE_LIMIT_EXCEEDED\",\"message\":\"Volumetric limit of %d req/sec reached. Please throttle requests.\",\"retryAfterSeconds\":1,\"path\":\"%s\"}",
                        Instant.now(), DEFAULT_MAX_REQUESTS_PER_SECOND, path
                );
                httpResponse.getWriter().write(jsonError);
                return;
            }
        }

        chain.doFilter(request, response);
    }

}
