package com.tresofertas.repository;

import com.tresofertas.model.OfferReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferReportRepo extends JpaRepository<OfferReport, Long> {
    long countByOfferId(Long offerId);
    List<OfferReport> findByOfferId(Long offerId);
    boolean existsByOfferIdAndReporterId(Long offerId, Long reporterId);
}
