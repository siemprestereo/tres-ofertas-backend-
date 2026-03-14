package com.example.waiter_rating.dto.response;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String activeRole,
        Boolean suspended,
        Boolean emailVerified,
        String authProvider,
        LocalDateTime createdAt,
        Integer totalRatings
) {}