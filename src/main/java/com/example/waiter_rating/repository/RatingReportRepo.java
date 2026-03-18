package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.RatingReport;
import com.example.waiter_rating.model.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingReportRepo extends JpaRepository<RatingReport, Long> {
    List<RatingReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<RatingReport> findAllByOrderByCreatedAtDesc();
    boolean existsByRatingIdAndReporterId(Long ratingId, Long reporterId);
    Optional<RatingReport> findByRatingIdAndReporterId(Long ratingId, Long reporterId);
}