package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Professional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessionalRepo extends JpaRepository<Professional, Long> {
    Optional<Professional> findByEmail(String email);


}