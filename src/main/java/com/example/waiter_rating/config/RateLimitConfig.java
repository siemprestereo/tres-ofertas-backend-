package com.example.waiter_rating.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Rate limiter para login/register: 5 intentos por minuto
     */
    public Bucket resolveAuthBucket(String key) {
        return cache.computeIfAbsent(key, k -> createAuthBucket());
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Rate limiter para APIs públicas: 100 requests por minuto
     */
    public Bucket resolvePublicApiBucket(String key) {
        return cache.computeIfAbsent(key, k -> createPublicApiBucket());
    }

    private Bucket createPublicApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Rate limiter para APIs autenticadas: 1000 requests por hora
     */
    public Bucket resolveAuthenticatedApiBucket(String key) {
        return cache.computeIfAbsent(key, k -> createAuthenticatedApiBucket());
    }

    private Bucket createAuthenticatedApiBucket() {
        Bandwidth limit = Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Rate limiter estricto para password reset: 3 intentos por hora
     */
    public Bucket resolvePasswordResetBucket(String key) {
        return cache.computeIfAbsent(key, k -> createPasswordResetBucket());
    }

    private Bucket createPasswordResetBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
