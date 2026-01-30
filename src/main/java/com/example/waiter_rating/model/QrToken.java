package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // Código único del QR

    // Relación con AppUser (Professional)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private AppUser professional;

    // Business donde se generó el QR (contexto del servicio)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Cuándo expira (ej: 3 minutos desde creación)

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true; // Se marca false después de usarse

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Verifica si el token es válido en este momento
     */
    public boolean isValidNow() {
        return active && expiresAt != null && LocalDateTime.now().isBefore(expiresAt);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
