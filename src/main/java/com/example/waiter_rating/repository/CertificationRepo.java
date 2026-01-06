package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificationRepo extends JpaRepository<Certification, Long> {
    List<Certification> findByProfessionalId(Long professionalId);
    void deleteByProfessionalId(Long professionalId);
}