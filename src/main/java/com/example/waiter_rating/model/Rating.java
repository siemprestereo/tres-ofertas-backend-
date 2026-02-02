package com.example.waiter_rating.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = true)
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private AppUser professional;

    // Negocio donde se realizó el servicio (contexto laboral)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // Lugar de trabajo específico donde se dio el servicio (NUEVO)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_history_id", nullable = true)
    private WorkHistory workHistory;

    // Puntaje 1..5
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int score;

    // Comentario opcional (máx. 140 chars)
    @Size(max = 140)
    @Column(length = 140)
    private String comment;

    // Fecha del servicio (cuando se prestó el servicio)
    @Column(name = "service_date")
    private LocalDateTime serviceDate;

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Regla de negocio: editar/eliminar solo dentro de 5 minutos desde la creación
     */
    public boolean canEditOrDelete() {
        if (this.createdAt == null) return false;
        LocalDateTime now = LocalDateTime.now();
        long minutesPassed = ChronoUnit.MINUTES.between(this.createdAt, now);
        return minutesPassed < 30; // Cambiar de 5 a 30
    }
}