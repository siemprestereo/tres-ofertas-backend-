package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.ProfessionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfessionalResponse {
    private Long id;
    private String name;
    private String email;
    private String profilePicture;
    private Boolean emailVerified;
    private String provider;
    private ProfessionType professionType; // NUEVO
    private LocalDateTime createdAt;

    // Datos de reputación
    private Double averageRating;
    private Integer totalRatings;

    // Control de cambios de trabajo
    private Integer monthlyWorkplaceChanges;
    private Boolean canChangeWorkplace;
}