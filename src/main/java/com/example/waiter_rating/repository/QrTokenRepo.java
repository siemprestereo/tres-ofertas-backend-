package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QrTokenRepo extends JpaRepository<QrToken, Long> {

    /** Buscar un QR token por su código */
    Optional<QrToken> findByCode(String code);

    /** Contar QR tokens activos de un professional */
    long countByProfessionalIdAndActiveTrue(Long professionalId);

    /** Invalidar todos los QR activos de un professional */
    @Modifying
    @Query("UPDATE QrToken t SET t.active = false " +
            "WHERE t.active = true AND t.professional.id = :professionalId")
    int invalidateAllActiveForProfessional(@Param("professionalId") Long professionalId);
}