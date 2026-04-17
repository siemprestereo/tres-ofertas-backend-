package com.tresofertas.repository;

import com.tresofertas.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepo extends JpaRepository<Offer, Long> {
    List<Offer> findByMerchantIdAndActiveTrue(Long merchantId);
    long countByMerchantIdAndActiveTrue(Long merchantId);
    Optional<Offer> findByCode(String code);
    boolean existsByCode(String code);
}
