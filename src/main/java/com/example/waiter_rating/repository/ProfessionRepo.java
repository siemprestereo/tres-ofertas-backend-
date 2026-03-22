package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Profession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionRepo extends JpaRepository<Profession, Long> {
    List<Profession> findAllByOrderByDisplayNameAsc();
    List<Profession> findByActiveTrueOrderByDisplayNameAsc();
    Optional<Profession> findByCode(String code);
    boolean existsByCode(String code);
}
