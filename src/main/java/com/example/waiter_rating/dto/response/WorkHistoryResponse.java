package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.BusinessType;
import lombok.Data;

@Data
public class WorkHistoryResponse {
    private Long id;
    private Long businessId;
    private String businessName;
    private BusinessType businessType;
    private String position;
    private String startDate;
    private String endDate;
    private Boolean isActive;
    private String referenceContact;
    private Boolean isFreelance = false;


    private Integer ratingsCountInPeriod;
    private Double avgScoreInPeriod;
}