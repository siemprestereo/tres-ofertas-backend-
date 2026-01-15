package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepo extends JpaRepository<Rating, Long> {

    // Todas las calificaciones de un professional (ordenadas por fecha)
    List<Rating> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);

    // Promedio de score de un professional
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.professional.id = :professionalId")
    Optional<Double> findAverageScoreByProfessionalId(Long professionalId);

    List<Rating> findByProfessionalId(Long professionalId);

    // Cantidad total de calificaciones de un professional
    long countByProfessionalId(Long professionalId);

    List<Rating> findByClientId(Long clientId);

    // Todas las calificaciones dadas por un cliente
    List<Rating> findByClientIdOrderByCreatedAtDesc(Long clientId);

    // Calificaciones de un professional en un business específico
    List<Rating> findByProfessionalIdAndBusinessId(Long professionalId, Long businessId);

    // Calificaciones de un professional en un business ordenadas por fecha
    List<Rating> findByProfessionalIdAndBusinessIdOrderByCreatedAtDesc(Long professionalId, Long businessId);

    //
    List<Rating> findByProfessionalIdAndWorkHistoryIdOrderByCreatedAtDesc(Long professionalId, Long workHistoryId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.professional.id = :professionalId")
    Double calculateAverageScore(@Param("professionalId") Long professionalId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.professional.id = :professionalId")
    Long countRatingsByProfessional(@Param("professionalId") Long professionalId);

         // Busca el último rating que un cliente le dio a un profesional
    @Query("SELECT r FROM Rating r WHERE r.client.id = :clientId AND r.professional.id = :professionalId ORDER BY r.createdAt DESC")
    Optional<Rating> findLastRatingByClientAndProfessional(@Param("clientId") Long clientId,
                                                           @Param("professionalId") Long professionalId);

    List<Rating> findByWorkHistoryId(Long workHistoryId);

    long countByWorkHistoryId(Long workHistoryId);
}


