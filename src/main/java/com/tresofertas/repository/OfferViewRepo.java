package com.tresofertas.repository;

import com.tresofertas.model.OfferView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferViewRepo extends JpaRepository<OfferView, Long> {
    long countByOfferId(Long offerId);
}
