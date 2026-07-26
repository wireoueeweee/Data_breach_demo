package com.example.breachdemo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal fixed-window rate limiter (defence in depth for v2).
 *
 * This is NOT the fix for the breach — object-level authorization is. Rate
 * limiting is a second layer: even if something else were misconfigured, a
 * client firing hundreds of requests per minute (the signature of an
 * enumeration attack) gets throttled with HTTP 429, buying time and making the
 * attack noisy. Keyed per authenticated user, falling back to client IP.
 *
 * Deliberately simple and in-memory; a production system would use a shared
 * store (e.g. Redis) and a smarter algorithm (token/leaky bucket).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.ratelimit.max-per-minute:60}")
    private int maxPerMinute;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        long minute;
        int count;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String key = keyFor(request);
        long currentMinute = System.currentTimeMillis() / 60_000L;

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                Window fresh = new Window();
                fresh.minute = currentMinute;
                fresh.count = 1;
                return fresh;
            }
            existing.count++;
            return existing;
        });

        if (window.count > maxPerMinute) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String keyFor(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
