package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.enums.ReportStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RatingReportResponse {
    private Long id;
    private Long ratingId;
    private Integer ratingScore;
    private String ratingComment;
    private String professionalName;
    private String clientName;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private ReportStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}