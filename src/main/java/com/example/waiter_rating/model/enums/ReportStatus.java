package com.example.waiter_rating.model.enums;

public enum ReportStatus {
    PENDING,
    APPROVED,   // denuncia aceptada → calificación eliminada
    REJECTED    // denuncia rechazada → calificación se mantiene
}