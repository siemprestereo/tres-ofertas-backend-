package com.tresofertas.repository;

import com.tresofertas.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepo extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Merchant> findByUserId(Long userId);
    List<Merchant> findByVerifiedFalseAndActiveTrue();
}
