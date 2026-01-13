package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.RatingFromQrRequest;
import com.example.waiter_rating.dto.request.RatingRequest;
import com.example.waiter_rating.dto.response.RatingResponse;
import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final AuthService authService;

    public RatingController(RatingService ratingService, AuthService authService) {
        this.ratingService = ratingService;
        this.authService = authService;
    }

    /** Crear una calificación (requiere autenticación como CLIENT) */
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody RatingRequest request) {
        // Verificar que el usuario está autenticado como cliente
        Client client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente para calificar"));

        // Automáticamente asignar el clientId del usuario logueado
        request.setClientId(client.getId());

        Rating r = ratingService.submitRating(request);
        return ResponseEntity.ok(toResponse(r));
    }

    /** Crear calificación desde QR (requiere autenticación como CLIENT) */
    @PostMapping("/from-qr/{code}")
    public ResponseEntity<?> submitFromQr(
            @PathVariable String code,
            @Valid @RequestBody RatingFromQrRequest request) {

        // Verificar que el usuario está autenticado como cliente
        Client client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente para calificar"));

        // Automáticamente asignar el clientId
        request.setClientId(client.getId());

        Rating r = ratingService.submitFromQr(code, request);
        return ResponseEntity.ok(toResponse(r));
    }

    /**
     * Editar MI calificación (solo el dueño, dentro de 5 minutos)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody RatingRequest request) {

        // Verificar que está autenticado como cliente
        Client client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente"));

        // Obtener el rating
        Rating rating = ratingService.getRatingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rating no encontrado"));

        // ✅ Verificar que el rating pertenece a este cliente
        if (rating.getClient() == null || !rating.getClient().getId().equals(client.getId())) {
            throw new IllegalStateException("No puede editar una calificación que no es suya");
        }

        // ✅ Verificar que está dentro de los 5 minutos
        if (!rating.canEditOrDelete()) {
            throw new IllegalStateException("Solo puede editar calificaciones dentro de los 5 minutos posteriores a su creación");
        }

        return ratingService.updateRating(id, request.getScore(), request.getComment())
                .map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Eliminar MI calificación (solo el dueño, dentro de 5 minutos)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // Verificar que está autenticado como cliente
        Client client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente"));

        // Obtener el rating
        Rating rating = ratingService.getRatingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rating no encontrado"));

        // ✅ Verificar que el rating pertenece a este cliente
        if (rating.getClient() == null || !rating.getClient().getId().equals(client.getId())) {
            throw new IllegalStateException("No puede eliminar una calificación que no es suya");
        }

        // ✅ Verificar que está dentro de los 5 minutos
        if (!rating.canEditOrDelete()) {
            throw new IllegalStateException("Solo puede eliminar calificaciones dentro de los 5 minutos posteriores a su creación");
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

    // ---------- Mapper entidad -> DTO ----------
    private RatingResponse toResponse(Rating r) {
        RatingResponse dto = new RatingResponse();
        dto.setId(r.getId());
        dto.setScore(r.getScore());
        dto.setComment(r.getComment());

        // Professional info
        dto.setProfessionalId(r.getProfessional().getId());
        dto.setProfessionalName(r.getProfessional().getName());
        dto.setProfessionType(r.getProfessional().getProfessionType());

        // Client info (puede ser null)
        if (r.getClient() != null) {
            dto.setClientId(r.getClient().getId());
            dto.setClientName(r.getClient().getName());
        }

        // Business info
        dto.setBusinessId(r.getBusiness().getId());
        dto.setBusinessName(r.getBusiness().getName());
        dto.setBusinessType(r.getBusiness().getBusinessType());

        // WorkHistory info (lugar específico donde fue calificado)
        if (r.getWorkHistory() != null) {
            dto.setWorkHistoryId(r.getWorkHistory().getId());
            dto.setWorkplaceName(r.getWorkHistory().getBusinessName());
            dto.setWorkplacePosition(r.getWorkHistory().getPosition());
        }

        // Timestamps
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setServiceDate(r.getServiceDate());

        // Can edit?
        dto.setCanEdit(r.canEditOrDelete());

        return dto;
    }

    // Agregar este método en tu RatingController.java

    /**
     * Obtener todas las calificaciones que el cliente logueado ha realizado
     */
    @GetMapping("/my-ratings")
    public ResponseEntity<List<RatingResponse>> getMyRatings() {
        // Obtener cliente logueado
        Client client = authService.getCurrentClient()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado como cliente"));

        // Obtener sus calificaciones
        List<Rating> ratings = ratingService.getRatingsByClient(client.getId());

        // Convertir a DTO
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
     * Este endpoint es para que el cliente vea sus propias calificaciones
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByClient(@PathVariable Long clientId) {
        // Obtener las calificaciones del cliente
        List<Rating> ratings = ratingService.getRatingsByClient(clientId);

        // Convertir a DTO
        List<RatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener calificaciones de un professional (PÚBLICO)
     * Opcionalmente filtradas por workplace
     */
    @GetMapping("/professional/{professionalId}/ratings")
    public ResponseEntity<List<RatingResponse>> getProfessionalRatings(
            @PathVariable Long professionalId,
            @RequestParam(required = false) Long workHistoryId) {

        List<Rating> ratings;

        if (workHistoryId != null) {
            // Filtrar por workplace específico
            ratings = ratingService.getRatingsByProfessionalAndWorkplace(professionalId, workHistoryId);
        } else {
            // Todos los ratings del professional
            ratings = ratingService.getRatingsForProfessional(professionalId);
        }

        List<RatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}