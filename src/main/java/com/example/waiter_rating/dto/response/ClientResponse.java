package com.example.waiter_rating.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientResponse {
    private Long id;
    private String name;
    private String email;
    private String profilePicture;
    private Boolean emailVerified;
    private String provider;
    private LocalDateTime createdAt;
    private Integer totalRatingsGiven; // Cantidad de calificaciones que dio
}