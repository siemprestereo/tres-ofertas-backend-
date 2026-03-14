package com.example.waiter_rating.dto.response;

public record AdminStatsResponse(
        Long totalUsers,
        Long totalProfessionals,
        Long totalClients,
        Long suspendedUsers,
        Long totalRatings,
        Double averageScore
) {}