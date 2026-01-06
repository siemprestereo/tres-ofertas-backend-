package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepo extends JpaRepository<Education, Long> {
    List<Education> findByProfessionalId(Long professionalId);
    void deleteByProfessionalId(Long professionalId);
}