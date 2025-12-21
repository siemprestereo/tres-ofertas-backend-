package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cvs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Professional dueño del CV (único) */
    @OneToOne
    @JoinColumn(name = "professional_id", nullable = false, unique = true)
    private Professional professional;

    /** Descripción general del perfil (texto libre) */
    @Column(length = 1000)
    private String description;

    /** Slug público para URL amigable (ej: /cv/juan-perez-electricista) */
    @Column(name = "public_slug", length = 64, unique = true)
    private String publicSlug;

    /** Datos de reputación (calculados automáticamente desde Professional) */
    @Column(name = "reputation_score")
    private Double reputationScore;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    /**
     * Actualiza la reputación basándose en los ratings del Professional.
     * Este método debe llamarse cada vez que el Professional recibe un nuevo rating.
     */
    public void updateReputationFromProfessional() {
        if (professional != null) {
            this.reputationScore = professional.getAverageRating();  // ← Simplificado
            this.totalRatings = professional.getTotalRatings();
        }
    }
}