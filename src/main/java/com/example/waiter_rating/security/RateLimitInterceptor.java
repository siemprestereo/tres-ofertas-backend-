package com.example.waiter_rating.security;

import com.example.waiter_rating.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    public RateLimitInterceptor(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = getClientIP(request);
        String path = request.getRequestURI();

        Bucket bucket;

        // Determinar qué bucket usar según el endpoint
        if (isAuthEndpoint(path)) {
            bucket = rateLimitConfig.resolveAuthBucket(key);
        } else if (isPasswordResetEndpoint(path)) {
            bucket = rateLimitConfig.resolvePasswordResetBucket(key);
        } else if (isPublicEndpoint(path)) {
            bucket = rateLimitConfig.resolvePublicApiBucket(key);
        } else {
            // APIs autenticadas
            String userId = request.getHeader("Authorization");
            if (userId != null) {
                bucket = rateLimitConfig.resolveAuthenticatedApiBucket(userId);
            } else {
                bucket = rateLimitConfig.resolvePublicApiBucket(key);
            }
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(
                String.format("{\"error\": \"Demasiadas solicitudes. Intentá nuevamente en %d segundos.\"}", waitForRefill)
            );
            
            System.out.println("⚠️ Rate limit exceeded for IP: " + key + " on path: " + path);
            
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private boolean isAuthEndpoint(String path) {
        return path.contains("/api/auth/login") || 
               path.contains("/api/auth/register");
    }

    private boolean isPasswordResetEndpoint(String path) {
        return path.contains("/api/auth/reset-password") || 
               path.contains("/api/auth/forgot-password");
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/api/cv/professional/") || 
               path.contains("/api/professionals/public") ||
               path.contains("/api/ratings/public");
    }
}
