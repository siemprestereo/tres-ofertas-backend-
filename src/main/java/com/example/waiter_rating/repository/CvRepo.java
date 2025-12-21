package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Cv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CvRepo extends JpaRepository<Cv, Long> {
    Optional<Cv> findByProfessionalId(Long professionalId);

    Optional<Cv> findByPublicSlug(String publicSlug);
}