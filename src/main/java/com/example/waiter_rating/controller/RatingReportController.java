package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.RatingReportRequest;
import com.example.waiter_rating.dto.request.ResolveReportRequest;
import com.example.waiter_rating.dto.response.RatingReportResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.RatingReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class RatingReportController {

    private final RatingReportService reportService;
    private final AuthService authService;

    public RatingReportController(RatingReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    /** Profesional denuncia una calificación recibida */
    @PostMapping("/ratings/{ratingId}")
    public ResponseEntity<RatingReportResponse> reportRating(
            @PathVariable Long ratingId,
            @Valid @RequestBody RatingReportRequest request) {

        AppUser professional = authService.getCurrentProfessional()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como profesional"));

        RatingReportResponse response = reportService.createReport(ratingId, professional.getId(), request);
        return ResponseEntity.ok(response);
    }

    /** Admin — listar todas las denuncias */
    @GetMapping("/admin")
    public ResponseEntity<List<RatingReportResponse>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    /** Admin — listar solo las pendientes */
    @GetMapping("/admin/pending")
    public ResponseEntity<List<RatingReportResponse>> getPendingReports() {
        return ResponseEntity.ok(reportService.getPendingReports());
    }

    /** Admin — resolver una denuncia (aprobar o rechazar) */
    @PatchMapping("/admin/{reportId}/resolve")
    public ResponseEntity<RatingReportResponse> resolveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ResolveReportRequest request) {

        return ResponseEntity.ok(reportService.resolveReport(reportId, request));
    }
}