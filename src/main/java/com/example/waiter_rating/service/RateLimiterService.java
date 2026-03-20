package com.example.waiter_rating.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 10 intentos por minuto por IP
    private Bucket newLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    // 5 registros por 10 minutos por IP
    private Bucket newRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(10))))
                .build();
    }

    // 3 intentos de forgot-password por 15 minutos por IP
    private Bucket newForgotPasswordBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(15))))
                .build();
    }

    public boolean tryConsumeLogin(String ip) {
        return buckets.computeIfAbsent("login:" + ip, k -> newLoginBucket()).tryConsume(1);
    }

    public boolean tryConsumeRegister(String ip) {
        return buckets.computeIfAbsent("register:" + ip, k -> newRegisterBucket()).tryConsume(1);
    }

    public boolean tryConsumeForgotPassword(String ip) {
        return buckets.computeIfAbsent("forgot:" + ip, k -> newForgotPasswordBucket()).tryConsume(1);
    }
}
