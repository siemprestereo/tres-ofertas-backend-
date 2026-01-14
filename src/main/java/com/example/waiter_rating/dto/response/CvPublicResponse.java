package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.ProfessionType;
import lombok.Data;

import java.util.List;

@Data
public class CvPublicResponse {
    private Long professionalId;
    private String professionalName;
    private String professionalEmail;
    private String professionalPhone; 
    private String professionalLocation;
    private String profilePicture;
    private ProfessionType professionType;

    private String description;
    private Double reputationScore;
    private Integer totalRatings;

    private List<CvExperienceItem> workHistory;
    private List<EducationResponse> education;
    private List<CertificationResponse> certifications;
}