package com.tresofertas.repository;

import com.tresofertas.model.MerchantView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantViewRepo extends JpaRepository<MerchantView, Long> {
    long countByMerchantId(Long merchantId);
}
