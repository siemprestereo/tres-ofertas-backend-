package com.example.waiter_rating.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CvPublicResponse {
    private Long professionalId;
    private String publicSlug;
    private String professionalName;
    private java.util.List<String> professionTypes;
    private String professionalEmail;
    private String professionalPhone;
    private String professionalLocation;
    private String profilePicture;
    private String professionType;
    private String professionalTitle;

    private String description;
    private String skills;
    private Double reputationScore;
    private Integer totalRatings;

    private List<ZoneResponse> zones;

    private List<CvExperienceItem> workHistory;
    private List<EducationResponse> education;
    private List<CertificationResponse> certifications;


}