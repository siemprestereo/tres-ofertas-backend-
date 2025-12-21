package com.example.waiter_rating.service;

import com.example.waiter_rating.model.Cv;

import java.util.Optional;

public interface CvService {

    /** Obtiene o crea el CV de un professional */
    Cv getOrCreateForProfessional(Long professionalId);

    /** Obtiene el CV público de un professional */
    Cv getPublicCv(Long professionalId);

    /** Obtiene CV por slug público */
    Optional<Cv> getByPublicSlug(String slug);

    /** Actualiza la descripción del CV */
    Cv updateDescription(Long professionalId, String description);

    /** Recalcula y actualiza el reputation score */
    Cv updateReputationScore(Long professionalId);

    /** Elimina un CV */
    void delete(Long cvId);
}