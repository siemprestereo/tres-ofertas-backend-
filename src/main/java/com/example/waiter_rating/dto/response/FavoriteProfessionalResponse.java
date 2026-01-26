package com.example.waiter_rating.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteProfessionalResponse {
    private Long favoriteId;
    private Long professionalId;
    private String professionalName;
    private String professionalEmail;
    private String professionType;
    private String profilePicture;
    private Double reputationScore;
    private Integer totalRatings;
    private LocalDateTime savedAt;
    private String notes;

    // Estadísticas adicionales (opcionales)
    private Double avgScoreLastMonth;
    private Integer ratingsLastMonth;
    private Double avgScoreLastThreeMonths;
    private Integer ratingsLastThreeMonths;
}