package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.request.RatingReportRequest;
import com.example.waiter_rating.dto.request.ResolveReportRequest;
import com.example.waiter_rating.dto.response.RatingReportResponse;

import java.util.List;

public interface RatingReportService {
    RatingReportResponse createReport(Long ratingId, Long reporterId, RatingReportRequest request);
    List<RatingReportResponse> getAllReports();
    List<RatingReportResponse> getPendingReports();
    RatingReportResponse resolveReport(Long reportId, ResolveReportRequest request);
}