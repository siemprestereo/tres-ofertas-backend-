package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.request.ProfessionalRequest;
import com.example.waiter_rating.dto.response.ProfessionalResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.ProfessionType;

import java.util.List;
import java.util.Optional;

public interface ProfessionalService {
    /** Crear un professional (registro) */
    ProfessionalResponse create(ProfessionalRequest request);
    
    /** Obtener professional por ID */
    Optional<ProfessionalResponse> getById(Long id);
    
    /** Obtener professional por email */
    Optional<ProfessionalResponse> getByEmail(String email);
    
    /** Listar todos los professionals */
    List<ProfessionalResponse> listAll();
    
    /** Listar professionals por tipo de profesión */
    List<ProfessionalResponse> listByProfessionType(ProfessionType professionType);
    
    /** Actualizar datos del professional */
    ProfessionalResponse update(Long id, ProfessionalRequest request);
    
    /** Eliminar professional */
    void delete(Long id);
    
    /** Verificar si el professional puede cambiar de lugar de trabajo este mes */
    boolean canChangeWorkplace(Long professionalId);
    
    /** Registrar un cambio de lugar de trabajo */
    void registerWorkplaceChange(Long professionalId);
    
    /** Buscar professional entity por email (para autenticación) */
    AppUser findByEmail(String email);
    
    AppUser findOrCreateFromGoogle(String email, String name, String googleId, Boolean emailVerified);
    
    /**
     * Recalcula y actualiza la reputación de un profesional
     * basándose en todos sus ratings actuales
     * @param professionalId ID del profesional a actualizar
     */
    void updateProfessionalReputation(Long professionalId);
}
