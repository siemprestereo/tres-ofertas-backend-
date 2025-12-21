package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.BusinessType;
import com.example.waiter_rating.model.ProfessionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RatingResponse {
    private Long id;
    private Integer score;
    private String comment;

    // Professional info
    private Long professionalId;
    private String professionalName;
    private ProfessionType professionType;

    // Client info
    private Long clientId;
    private String clientName;

    // Business info
    private Long businessId;
    private String businessName;
    private BusinessType businessType;

    // WorkHistory info (lugar específico donde fue calificado)
    private Long workHistoryId;
    private String workplaceName;  // businessName del WorkHistory
    private String workplacePosition; // position del WorkHistory

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime serviceDate;

    // Permisos
    private Boolean canEdit;
}