package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.request.ClientRequest;
import com.example.waiter_rating.dto.response.ClientResponse;
import com.example.waiter_rating.model.AppUser;

import java.util.List;
import java.util.Optional;

public interface ClientService {
    /** Crear un cliente (registro) */
    ClientResponse create(ClientRequest request);
    
    /** Obtener cliente por ID */
    Optional<ClientResponse> getById(Long id);
    
    /** Obtener cliente por email */
    Optional<ClientResponse> getByEmail(String email);
    
    /** Listar todos los clientes */
    List<ClientResponse> listAll();
    
    /** Actualizar datos del cliente */
    ClientResponse update(Long id, ClientRequest request);
    
    /** Eliminar cliente */
    void delete(Long id);
    
    /** Buscar o crear cliente desde OAuth2 Google */
    AppUser findOrCreateFromGoogle(String email, String name, String googleId, Boolean emailVerified);
    
    AppUser findByEmail(String email);
    
    /**
     * Convierte un Cliente en Profesional
     */
    AppUser upgradeToProfessional(Long clientId, String professionType, String professionalTitle);
}
