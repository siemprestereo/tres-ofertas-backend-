package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.RatingFromQrRequest;
import com.example.waiter_rating.dto.request.RatingRequest;
import com.example.waiter_rating.dto.response.RatingResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.repository.RatingReportRepo;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.RatingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
@Slf4j
public class RatingController {

    private final RatingService ratingService;
    private final AuthService authService;

    private final RatingReportRepo ratingReportRepo;
    public RatingController(RatingService ratingService, AuthService authService, RatingReportRepo ratingReportRepo) {
        this.ratingService = ratingService;
        this.authService = authService;
        this.ratingReportRepo = ratingReportRepo;
    }

    /** Crear una calificación (requiere autenticación como CLIENT) */
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody RatingRequest request) {
        AppUser client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente para calificar"));

        request.setClientId(client.getId());

        Rating r = ratingService.submitRating(request);
        RatingResponse resp = toResponse(r);
        if (request.getComment() != null && !request.getComment().isBlank() && r.getComment() == null) {
            resp.setCommentModerated(true);
        }
        return ResponseEntity.ok(resp);
    }

    /** Crear calificación desde QR (requiere autenticación como CLIENT) */
    @PostMapping("/from-qr/{code}")
    public ResponseEntity<?> submitFromQr(
            @PathVariable String code,
            @Valid @RequestBody RatingFromQrRequest request) {

        AppUser client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente para calificar"));

        request.setClientId(client.getId());

        Rating r = ratingService.submitFromQr(code, request);
        RatingResponse resp = toResponse(r);
        if (request.getComment() != null && !request.getComment().isBlank() && r.getComment() == null) {
            resp.setCommentModerated(true);
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * Editar MI calificación (solo el dueño, dentro de 30 minutos)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequest request) {

        AppUser client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente"));

        Rating rating = ratingService.getRatingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rating no encontrado"));

        if (rating.getClient() == null || !rating.getClient().getId().equals(client.getId())) {
            throw new IllegalStateException("No puede editar una calificación que no es suya");
        }

        if (!rating.canEditOrDelete()) {
            throw new IllegalStateException("Solo puede editar calificaciones dentro de los 30 minutos posteriores a su creación");
        }

        String originalComment = request.getComment();
        return ratingService.updateRating(id, request.getScore(), originalComment)
                .map(r -> {
                    RatingResponse resp = toResponse(r);
                    if (originalComment != null && !originalComment.isBlank() && r.getComment() == null) {
                        resp.setCommentModerated(true);
                    }
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Eliminar MI calificación (solo el dueño, dentro de 30 minutos)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        AppUser client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente"));

        Rating rating = ratingService.getRatingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rating no encontrado"));

        if (rating.getClient() == null || !rating.getClient().getId().equals(client.getId())) {
            throw new IllegalStateException("No puede eliminar una calificación que no es suya");
        }

        if (!rating.canEditOrDelete()) {
            throw new IllegalStateException("Solo puede eliminar calificaciones dentro de los 30 minutos posteriores a su creación");
        }

        boolean deleted = ratingService.deleteRating(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Listar calificaciones de un professional (PÚBLICO) */
    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<RatingResponse>> listByProfessional(@PathVariable Long professionalId) {
        List<RatingResponse> out = ratingService.getRatingsForProfessional(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    /** Promedio de calificaciones de un professional (PÚBLICO) */
    @GetMapping("/professional/{professionalId}/average")
    public ResponseEntity<Double> average(@PathVariable Long professionalId) {
        return ResponseEntity.ok(ratingService.getAverageScoreForProfessional(professionalId));
    }

    /**
     * Obtener todas las calificaciones que el cliente logueado ha realizado
     */
    @GetMapping("/my-ratings")
    public ResponseEntity<List<RatingResponse>> getMyRatings() {
        AppUser client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente"));

        List<Rating> ratings = ratingService.getRatingsByClient(client.getId());

        List<RatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener ratings por workHistoryId (PÚBLICO - para compartir CV)
     */
    @GetMapping("/work-history/{workHistoryId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByWorkHistory(@PathVariable Long workHistoryId) {
        List<Rating> ratings = ratingService.getRatingsByWorkHistory(workHistoryId);

        List<RatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener todas las calificaciones de un cliente específico
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByClient(
            @PathVariable Long clientId,
            @RequestParam(required = false) Integer limit) {

        // SEGURIDAD: Validar que el usuario solo pueda ver sus propios ratings
        AppUser currentUser = authService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Usuario no autenticado"));

        if (!currentUser.getId().equals(clientId)) {
            log.warn("Intento de acceso no autorizado al historial del cliente {} por parte del usuario {}", clientId, currentUser.getId());
            throw new IllegalStateException("No tienes permiso para ver el historial de otro usuario");
        }

        List<Rating> ratings;
        if (limit != null && limit > 0) {
            ratings = ratingService.getRatingsByClient(clientId, limit);
            log.info("Ratings solicitados con limit={} para clientId={}", limit, clientId);
        } else {
            ratings = ratingService.getRatingsByClient(clientId);
            log.info("Historial completo solicitado para clientId={}", clientId);
        }

        List<RatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener calificaciones de un professional (PÚBLICO)
     */
    @GetMapping("/professional/{professionalId}/ratings")
    public ResponseEntity<List<RatingResponse>> getProfessionalRatings(
            @PathVariable Long professionalId,
            @RequestParam(required = false) Long workHistoryId) {

        List<Rating> ratings;
        if (workHistoryId != null) {
            ratings = ratingService.getRatingsByProfessionalAndWorkplace(professionalId, workHistoryId);
        } else {
            ratings = ratingService.getRatingsForProfessional(professionalId);
        }

        List<RatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ---------- Mapper entidad -> DTO ----------
    private RatingResponse toResponse(Rating r) {
        RatingResponse dto = new RatingResponse();
        dto.setId(r.getId());
        dto.setScore(r.getScore());
        dto.setComment(r.getComment());

        // Professional info — usar snapshot si el profesional fue eliminado
        if (r.getProfessional() != null) {
            dto.setProfessionalId(r.getProfessional().getId());
            dto.setProfessionalName(r.getProfessional().getName());
            dto.setProfessionType(r.getProfessional().getProfessionType());
        } else {
            dto.setProfessionalId(null);
            dto.setProfessionalName(r.getProfessionalName() != null ? r.getProfessionalName() : "Profesional eliminado");
            dto.setProfessionType(null);
        }

        // Client info
        if (r.getClient() != null) {
            dto.setClientId(r.getClient().getId());
            String fullName = r.getClient().getName();
            if (fullName != null && !fullName.isBlank()) {
                dto.setClientName(fullName.trim().split("\\s+")[0]);
            } else {
                dto.setClientName("Usuario");
            }
        }

        // Business info
        dto.setBusinessId(r.getBusiness().getId());
        dto.setBusinessName(r.getBusiness().getName());
        dto.setBusinessType(r.getBusiness().getBusinessType());

        // WorkHistory info
        if (r.getWorkHistory() != null) {
            dto.setWorkHistoryId(r.getWorkHistory().getId());
            dto.setWorkplaceName(r.getWorkHistory().getBusinessName());
            dto.setWorkplacePosition(r.getWorkHistory().getPosition());
        }

        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setServiceDate(r.getServiceDate());
        dto.setCanEdit(r.canEditOrDelete());
        ratingReportRepo.findFirstByRatingIdOrderByCreatedAtDesc(r.getId())
                .ifPresent(report -> dto.setReportStatus(report.getStatus().name()));

        return dto;
    }
}