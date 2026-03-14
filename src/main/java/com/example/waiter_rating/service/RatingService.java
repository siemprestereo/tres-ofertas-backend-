package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.request.RatingFromQrRequest;
import com.example.waiter_rating.dto.request.RatingRequest;
import com.example.waiter_rating.dto.response.AdminRatingResponse;
import com.example.waiter_rating.model.Rating;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

public interface RatingService {

    /** Crea una calificación clásica (no QR) */
    Rating submitRating(@Valid RatingRequest request);

    /** Crea una calificación a partir de un QR (professional sale del QR; business lo envía el cliente) */
    Rating submitFromQr(String code, @Valid RatingFromQrRequest request);

    /** Actualiza score/comentario solo si está dentro de 5 minutos desde createdAt */
    Optional<Rating> updateRating(Long id, Integer newScore, String newComment);

    /** Elimina la calificación solo si está dentro de 5 minutos desde createdAt */
    boolean deleteRating(Long id);

    /** Trae todas las calificaciones de un professional */
    List<Rating> getRatingsForProfessional(Long professionalId);

    /** Retorna el promedio de calificaciones de un professional (0.0 si no tiene) */
    double getAverageScoreForProfessional(Long professionalId);

    /** Obtener un rating por ID */
    Optional<Rating> getRatingById(Long id);

    /**
     * Obtener todas las calificaciones realizadas por un cliente específico
     */
    List<Rating> getRatingsByClient(Long clientId);

    /**
     * Obtener calificaciones de un professional en un workplace específico
     */
    List<Rating> getRatingsByProfessionalAndWorkplace(Long professionalId, Long workHistoryId);

    List<Rating> getRatingsByWorkHistory(Long workHistoryId);

    List<Rating> getRatingsByClient(Long clientId, Integer limit);

    // Admin
    List<AdminRatingResponse> listAllForAdmin();
    void deleteByAdmin(Long id);

}