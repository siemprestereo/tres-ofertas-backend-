package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.BusinessType;
import lombok.Data;

@Data
public class CvExperienceItem {
    private Long workHistoryId;
    private String businessName;
    private BusinessType businessType;
    private String position;
    private String description;
    private String startDate;
    private String endDate;
    private Boolean isActive;
    private Boolean isFreelance; // ← AGREGAR ESTA LÍNEA
    private String referenceContact;
    private String referencePhone;
    private Integer totalRatings;
}