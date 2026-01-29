package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.response.AppUserResponse;

import java.util.List;
import java.util.Map;

public interface AppUserService {

    /** Devuelve un usuario por id (mapeado a DTO de salida) */
    AppUserResponse getById(Long id);

    /** Lista todos los usuarios (mapeados a DTO de salida) */
    List<AppUserResponse> listAll();

    /**
     * Verifica los roles disponibles del usuario autenticado
     *
     * @param authHeader Header de autorización con el token JWT (formato: "Bearer {token}")
     * @return Map conteniendo:
     *         - hasClientRole (Boolean): si el usuario tiene tipo CLIENT
     *         - hasProfessionalRole (Boolean): si el usuario tiene tipo PROFESSIONAL
     *         - activeRole (String): rol actualmente activo ("CLIENT" o "PROFESSIONAL")
     *         - canSwitchRole (Boolean): si puede cambiar de rol (restricción de 6 meses)
     *         - nextAllowedSwitchDate (LocalDateTime): fecha del próximo cambio permitido (solo si canSwitchRole es false)
     * @throws RuntimeException si el token es inválido o el usuario no existe
     */
    Map<String, Object> checkUserRoles(String authHeader);
}