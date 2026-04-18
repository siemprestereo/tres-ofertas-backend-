package com.tresofertas.repository;

import com.tresofertas.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfferRepo extends JpaRepository<Offer, Long> {
    List<Offer> findByMerchantIdAndActiveTrue(Long merchantId);
    long countByMerchantIdAndActiveTrue(Long merchantId);
    Optional<Offer> findByCode(String code);
    boolean existsByCode(String code);

    @Query("SELECT o FROM Offer o JOIN FETCH o.merchant m " +
           "WHERE o.active = true AND m.active = true AND m.suspended = false " +
           "AND (o.untilStockOut = true OR o.expiresAt > :now) " +
           "AND (:category IS NULL OR m.category = :category) " +
           "ORDER BY o.createdAt DESC")
    List<Offer> findPublicFeed(@Param("now") LocalDateTime now, @Param("category") String category);

    @Query("SELECT o FROM Offer o JOIN FETCH o.merchant m " +
           "WHERE o.active = true AND m.active = true AND m.suspended = false " +
           "AND (o.untilStockOut = true OR o.expiresAt > :now) " +
           "AND m.id IN (SELECT f.merchant.id FROM Follow f WHERE f.consumer.id = :userId) " +
           "ORDER BY o.createdAt DESC")
    List<Offer> findPersonalizedFeed(@Param("now") LocalDateTime now, @Param("userId") Long userId);
}
