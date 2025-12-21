package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "professionals")
@DiscriminatorValue("PROFESSIONAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professional extends AppUser {

    @Enumerated(EnumType.STRING)
    @Column(name = "profession_type", nullable = false)
    private ProfessionType professionType = ProfessionType.WAITER;

    @Column(name = "reputation_score")
    @Builder.Default  // ← AGREGAR ESTO
    private Double reputationScore = 0.0;  // ← Inicializar con 0.0

    @Column(name = "total_ratings")
    @Builder.Default  // ← AGREGAR ESTO
    private Integer totalRatings = 0;  // ← Inicializar con 0

    @OneToOne(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cv cv;

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    private List<Rating> ratingsReceived = new ArrayList<>();

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    private List<WorkHistory> workHistory = new ArrayList<>();

    // Control de cambios de trabajo (máx 2 por mes)
    @Column(nullable = false)
    @Builder.Default
    private Integer monthlyWorkplaceChanges = 0;

    @Column
    private LocalDate lastWorkplaceChangeDate;

    @Override
    public String getUserType() {
        return "PROFESSIONAL";
    }

    // Métodos helper para reputación
    public Double getAverageRating() {
        if (ratingsReceived == null || ratingsReceived.isEmpty()) {
            return 0.0;
        }
        return ratingsReceived.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
    }

    public Integer getTotalRatingsCount() {
        return ratingsReceived != null ? ratingsReceived.size() : 0;
    }

    // Obtener lugares de trabajo actuales
    public List<Business> getCurrentWorkplaces() {
        if (workHistory == null) {
            return new ArrayList<>();
        }
        return workHistory.stream()
                .filter(WorkHistory::getIsActive)
                .map(WorkHistory::getBusiness)
                .toList();
    }

    // Validar si puede cambiar de trabajo este mes
    public boolean canChangeWorkplace() {
        if (lastWorkplaceChangeDate == null) {
            return true;
        }

        LocalDate now = LocalDate.now();
        // Si es un mes diferente, resetear contador
        if (lastWorkplaceChangeDate.getMonth() != now.getMonth() ||
                lastWorkplaceChangeDate.getYear() != now.getYear()) {
            return true;
        }

        // Dentro del mismo mes, verificar límite
        return monthlyWorkplaceChanges < 3;
    }

    public void registerWorkplaceChange() {
        LocalDate now = LocalDate.now();

        if (lastWorkplaceChangeDate == null ||
                lastWorkplaceChangeDate.getMonth() != now.getMonth() ||
                lastWorkplaceChangeDate.getYear() != now.getYear()) {
            // Nuevo mes, resetear
            monthlyWorkplaceChanges = 1;
        } else {
            monthlyWorkplaceChanges++;
        }

        lastWorkplaceChangeDate = now;
    }
}