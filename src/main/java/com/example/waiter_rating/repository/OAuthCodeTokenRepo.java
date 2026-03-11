package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.OAuthCodeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthCodeTokenRepo extends JpaRepository<OAuthCodeToken, Long> {

    Optional<OAuthCodeToken> findByCodeAndUsedFalse(String code);

    /**
     * Limpieza automática: elimina códigos expirados o ya usados.
     * Podés llamar esto desde un @Scheduled o manualmente.
     */
    @Modifying
    @Query("DELETE FROM OAuthCodeToken t WHERE t.used = true OR t.expiryDate < :now")
    int deleteExpiredOrUsed(LocalDateTime now);

    List<OAuthCodeToken> findByUserId(Long userId);
}