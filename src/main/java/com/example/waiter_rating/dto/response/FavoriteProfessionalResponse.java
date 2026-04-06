package com.example.waiter_rating.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteProfessionalResponse {
    private Long favoriteId;
    private Long professionalId;
    private String publicSlug;
    private java.util.List<String> professionTypes;
    private String professionalName;
    private String professionalEmail;
    private String professionType;
    private String profilePicture;
    private Double reputationScore;
    private Integer totalRatings;
    private LocalDateTime savedAt;
    private String notes;
    private List<ZoneResponse> zones; //added


    private List<WorkHistoryResponse> workHistory;
}