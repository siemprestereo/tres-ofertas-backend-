package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.FavoriteProfessional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteProfessionalRepo extends JpaRepository<FavoriteProfessional, Long> {

    // Verificar si un profesional está en favoritos
    boolean existsByClientIdAndProfessionalId(Long clientId, Long professionalId);

    // Obtener favorito específico
    Optional<FavoriteProfessional> findByClientIdAndProfessionalId(Long clientId, Long professionalId);

    // Listar todos los favoritos de un cliente
    List<FavoriteProfessional> findByClientIdOrderBySavedAtDesc(Long clientId);

    // Contar favoritos de un cliente
    long countByClientId(Long clientId);

    // Eliminar favorito
    void deleteByClientIdAndProfessionalId(Long clientId, Long professionalId);
}