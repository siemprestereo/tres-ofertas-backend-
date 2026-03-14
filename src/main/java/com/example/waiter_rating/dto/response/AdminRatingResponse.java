package com.example.waiter_rating.dto.response;

import java.time.LocalDateTime;

public record AdminRatingResponse(
        Long id,
        String clientName,
        String professionalName,
        Integer score,
        String comment,
        LocalDateTime createdAt
) {}