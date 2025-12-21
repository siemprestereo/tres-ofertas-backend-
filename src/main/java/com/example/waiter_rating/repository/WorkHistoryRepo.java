package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.WorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkHistoryRepo extends JpaRepository<WorkHistory, Long> {

    // Buscar historial laboral de un professional
    List<WorkHistory> findByProfessionalId(Long professionalId);

    // Buscar historial laboral activo de un professional
    List<WorkHistory> findByProfessionalIdAndIsActiveTrue(Long professionalId);

    // Verificar si un professional tiene (o tuvo) experiencia en un business
    boolean existsByProfessionalIdAndBusinessId(Long professionalId, Long businessId);

    // Verificar si un professional trabaja actualmente en un business
    boolean existsByProfessionalIdAndBusinessIdAndIsActiveTrue(Long professionalId, Long businessId);

    // Buscar historial en un business específico
    List<WorkHistory> findByBusinessId(Long businessId);
}