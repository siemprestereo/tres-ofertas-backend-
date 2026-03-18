package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.RatingReportRequest;
import com.example.waiter_rating.dto.request.ResolveReportRequest;
import com.example.waiter_rating.dto.response.RatingReportResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.model.RatingReport;
import com.example.waiter_rating.model.enums.ReportStatus;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.repository.RatingReportRepo;
import com.example.waiter_rating.service.RatingReportService;
import com.example.waiter_rating.service.RatingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class RatingReportServiceImpl implements RatingReportService {

    private final RatingReportRepo reportRepo;
    private final RatingRepo ratingRepo;
    private final AppUserRepo appUserRepo;
    private final RatingService ratingService;

    public RatingReportServiceImpl(RatingReportRepo reportRepo,
                                   RatingRepo ratingRepo,
                                   AppUserRepo appUserRepo,
                                   RatingService ratingService) {
        this.reportRepo = reportRepo;
        this.ratingRepo = ratingRepo;
        this.appUserRepo = appUserRepo;
        this.ratingService = ratingService;
    }

    @Override
    @Transactional
    public RatingReportResponse createReport(Long ratingId, Long reporterId, RatingReportRequest request) {
        Rating rating = ratingRepo.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("Calificación no encontrada"));

        AppUser reporter = appUserRepo.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Un profesional solo puede denunciar sus propias calificaciones
        if (rating.getProfessional() == null || !rating.getProfessional().getId().equals(reporterId)) {
            throw new IllegalStateException("Solo podés denunciar calificaciones recibidas en tu perfil");
        }

        // No puede denunciar dos veces la misma calificación
        if (reportRepo.existsByRatingIdAndReporterId(ratingId, reporterId)) {
            throw new IllegalStateException("Ya denunciaste esta calificación anteriormente");
        }

        RatingReport report = RatingReport.builder()
                .rating(rating)
                .reporter(reporter)
                .reason(request.getReason())
                .status(ReportStatus.PENDING)
                .build();

        RatingReport saved = reportRepo.save(report);
        log.info("Nueva denuncia creada: ratingId={}, reporterId={}", ratingId, reporterId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingReportResponse> getAllReports() {
        return reportRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingReportResponse> getPendingReports() {
        return reportRepo.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public RatingReportResponse resolveReport(Long reportId, ResolveReportRequest request) {
        RatingReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Denuncia no encontrada"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException("Esta denuncia ya fue resuelta");
        }

        report.setStatus(request.getStatus());
        report.setAdminNotes(request.getAdminNotes());
        report.setResolvedAt(LocalDateTime.now());

        // Si se aprueba la denuncia → eliminar la calificación
        if (request.getStatus() == ReportStatus.APPROVED) {
            ratingService.deleteByAdmin(report.getRating().getId());
            log.info("Calificación {} eliminada por denuncia aprobada {}", report.getRating().getId(), reportId);
        }

        RatingReport resolved = reportRepo.save(report);
        return toResponse(resolved);
    }

    private RatingReportResponse toResponse(RatingReport r) {
        RatingReportResponse dto = new RatingReportResponse();
        dto.setId(r.getId());
        dto.setRatingId(r.getRating().getId());
        dto.setRatingScore(r.getRating().getScore());
        dto.setRatingComment(r.getRating().getComment());
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());
        dto.setAdminNotes(r.getAdminNotes());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setResolvedAt(r.getResolvedAt());

        // Professional info
        if (r.getRating().getProfessional() != null) {
            dto.setProfessionalName(r.getRating().getProfessional().getName());
        } else {
            dto.setProfessionalName(r.getRating().getProfessionalName());
        }

        // Client info
        if (r.getRating().getClient() != null) {
            dto.setClientName(r.getRating().getClient().getName());
        } else {
            dto.setClientName("Anónimo");
        }

        // Reporter info
        dto.setReporterId(r.getReporter().getId());
        dto.setReporterName(r.getReporter().getName());

        return dto;
    }
}