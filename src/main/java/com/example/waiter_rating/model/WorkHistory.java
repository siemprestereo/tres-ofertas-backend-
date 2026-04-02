package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private AppUser professional;

    @ManyToOne(optional = true)
    @JoinColumn(name = "business_id")
    private Business business;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(length = 100)
    private String position; // "Mesero", "Electricista Senior", etc

    @Column(columnDefinition = "TEXT")
    private String description;  // ← NUEVO: Descripción del puesto, responsabilidades, logros

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate; // null = trabaja actualmente

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // true si endDate == null

    @Column(length = 200)
    private String referenceContact; // opcional

    @Column(name = "reference_phone", length = 100)
    private String referencePhone; // opcional

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_freelance", nullable = false)
    private Boolean isFreelance = false;

    // Método helper para verificar si está trabajando actualmente
    public boolean isCurrentJob() {
        return endDate == null && isActive;
    }


    // Actualizar el estado cuando se cierra la experiencia laboral
    public void closeJob(LocalDate endDate) {
        this.endDate = endDate;
        this.isActive = false;
    }
}