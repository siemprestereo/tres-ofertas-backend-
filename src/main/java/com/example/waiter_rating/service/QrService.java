package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.response.QrCreateResponse;

public interface QrService {

    /** Crea un QR dinámico para un professional con TTL en minutos */
    QrCreateResponse createDynamic(Long professionalId, Long businessId, int ttlMinutes);

    /** Resuelve el ID del professional desde el código QR */
    Long resolveProfessional(String code);

    /** Invalida un QR (lo marca como inactivo) */
    void invalidate(String code);
}