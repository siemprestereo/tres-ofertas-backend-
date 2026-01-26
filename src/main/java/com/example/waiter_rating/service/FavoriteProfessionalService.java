package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.response.FavoriteProfessionalResponse;

import java.time.LocalDate;
import java.util.List;

public interface FavoriteProfessionalService {

    // Agregar profesional a favoritos
    FavoriteProfessionalResponse addFavorite(Long clientId, Long professionalId, String notes);

    // Quitar de favoritos
    void removeFavorite(Long clientId, Long professionalId);

    // Verificar si está en favoritos
    boolean isFavorite(Long clientId, Long professionalId);

    // Listar favoritos
    List<FavoriteProfessionalResponse> listFavorites(Long clientId);

    // Listar favoritos con estadísticas filtradas
    List<FavoriteProfessionalResponse> listFavoritesWithStats(
            Long clientId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Actualizar notas
    FavoriteProfessionalResponse updateNotes(Long clientId, Long professionalId, String notes);

    // Obtener conteo
    long countFavorites(Long clientId);
}