package com.tresofertas.repository;

import com.tresofertas.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepo extends JpaRepository<Follow, Long> {
    boolean existsByConsumerIdAndMerchantId(Long consumerId, Long merchantId);
    Optional<Follow> findByConsumerIdAndMerchantId(Long consumerId, Long merchantId);
    List<Follow> findByConsumerId(Long consumerId);
    long countByMerchantId(Long merchantId);
}
