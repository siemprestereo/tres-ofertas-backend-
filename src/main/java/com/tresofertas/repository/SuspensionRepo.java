package com.tresofertas.repository;

import com.tresofertas.model.Suspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuspensionRepo extends JpaRepository<Suspension, Long> {
    List<Suspension> findByMerchantId(Long merchantId);
    long countByMerchantId(Long merchantId);
}
