package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepo extends JpaRepository<Professional, Long> {
    Optional<Professional> findByEmail(String email);

    @Query("SELECT p FROM Professional p WHERE " +
            "p.searchable = true AND " +
            "(LOWER(p.professionalName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.professionType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.cv.currentBusiness) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Professional> searchSearchableProfessionals(@Param("searchTerm") String searchTerm);


}