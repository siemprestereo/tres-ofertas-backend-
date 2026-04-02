package com.example.waiter_rating.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfessionalResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String profilePicture;
    private Boolean emailVerified;
    private String provider;
    private String professionType;
    private LocalDateTime createdAt;
    private String location;
    private String professionalTitle;

    // Datos de reputación
    private Double averageRating;
    private Integer totalRatings;

    // Control de cambios de trabajo
    private Integer monthlyWorkplaceChanges;
    private Boolean canChangeWorkplace;
}